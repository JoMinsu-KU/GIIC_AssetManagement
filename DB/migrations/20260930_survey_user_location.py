"""Reconcile surveyed asset users and locations without changing asset statuses."""

import argparse
import collections
import datetime
import json
import subprocess
from pathlib import Path

import openpyxl


CONTAINER = "asset_management_db"
SYNERGY_HALL = "PI-LAM 시너지홀(한마관 지상3층)"
LOCATION_OVERRIDES = {str(number): SYNERGY_HALL for number in range(2600249, 2600258)}
SPECIAL_USERS = {"미사용": "UNUSED", "사용안함": "UNUSED", "고장": "ABNORMAL"}
KNOWN_UNUSED_CONFLICTS = {"2600473", "2600479", "2600525", "2600526"}


def docker_mysql(sql):
    command = [
        "docker", "exec", "-i", CONTAINER, "sh", "-ec",
        'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" '
        '--default-character-set=utf8mb4 "$MYSQL_DATABASE" -B -N',
    ]
    result = subprocess.run(command, input=sql.encode("utf-8"), capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace"))
    return result.stdout.decode("utf-8").splitlines()


def read_workbook(path):
    sheet = openpyxl.load_workbook(path, read_only=True, data_only=True).active
    rows = sheet.iter_rows(values_only=True)
    headers = [str(value).strip() if value is not None else "" for value in next(rows)]
    required = ["자산번호", "비치장소", "상태", "실사용자"]
    if any(header not in headers for header in required):
        raise ValueError(f"Missing columns: {set(required) - set(headers)}")
    indexes = {header: headers.index(header) for header in required}
    assets = {}
    for row in rows:
        raw_number = row[indexes["자산번호"]]
        if raw_number is None:
            continue
        asset_no = str(int(raw_number)) if isinstance(raw_number, (int, float)) else str(raw_number).strip()
        if asset_no in assets:
            raise ValueError(f"Duplicate asset number in workbook: {asset_no}")
        location = str(row[indexes["비치장소"]] or "").strip()
        status = str(row[indexes["상태"]] or "").strip()
        user = str(row[indexes["실사용자"]] or "").strip()
        if not location or len(location) > 255:
            raise ValueError(f"Invalid location for {asset_no}")
        if status not in {"정상", "손망"}:
            raise ValueError(f"Unexpected survey status for {asset_no}: {status}")
        if status == "손망" and user:
            raise ValueError(f"Lost asset has a user in workbook: {asset_no}")
        if status == "손망":
            kind, expected_status = "empty", "LOST"
        elif user in SPECIAL_USERS:
            kind, expected_status = "special", SPECIAL_USERS[user]
        elif user == "공용":
            kind, expected_status = "shared", None
        elif user:
            kind, expected_status = "person", "NORMAL"
        else:
            kind, expected_status = "empty", "NORMAL"
        assets[asset_no] = {
            "location": LOCATION_OVERRIDES.get(asset_no, location),
            "survey_location": location,
            "user": user,
            "kind": kind,
            "expected_status": expected_status,
        }
    return assets


def decode_hex(value):
    return bytes.fromhex(value).decode("utf-8") if value else ""


def read_db():
    rows = docker_mysql(
        "SELECT asset_no, status, IFNULL(HEX(location), ''), "
        "IFNULL(HEX(actual_user_name), ''), shared_use+0 "
        "FROM assets ORDER BY asset_no;"
    )
    db = {}
    for row in rows:
        asset_no, status, location, user, shared = row.split("\t")
        db[asset_no] = {
            "status": status,
            "location": decode_hex(location).strip(),
            "user": decode_hex(user).strip(),
            "shared": shared == "1",
        }
    return db


def compare(assets, db):
    missing = sorted(set(assets) - set(db))
    if missing:
        raise ValueError(f"Workbook assets missing from DB: {missing}")
    counts = collections.Counter()
    for asset_no, source in assets.items():
        current = db[asset_no]
        kind = source["kind"]
        expected = source["expected_status"]
        known_conflict = (
            asset_no in KNOWN_UNUSED_CONFLICTS
            and kind in {"person", "shared"}
            and current["status"] == "UNUSED"
        )
        if expected and current["status"] != expected and not known_conflict:
            raise ValueError(f"Unexpected DB status for {asset_no}: {current['status']} != {expected}")
        if kind == "shared" and current["status"] not in {"NORMAL", "UNUSED"}:
            raise ValueError(f"Unexpected shared asset status for {asset_no}: {current['status']}")
        if current["location"] != source["location"]:
            counts["location_mismatch"] += 1
        if source["survey_location"] != source["location"]:
            counts["location_overrides"] += 1
        if known_conflict:
            counts[f"{kind}_unused_conflict"] += 1
            continue
        if kind == "person" and (current["user"] != source["user"] or current["shared"]):
            counts["person_mismatch"] += 1
        elif kind == "shared" and (not current["shared"] or current["user"]):
            counts["shared_mismatch"] += 1
        elif kind == "empty" and current["status"] == "NORMAL" and (current["user"] or current["shared"]):
            counts["empty_mismatch"] += 1
    return counts


def sql_value(value):
    return "0x" + value.encode("utf-8").hex() if value else "''"


def build_sql(assets, db, resolve_unused):
    values = []
    for asset_no, source in assets.items():
        values.append(
            f"({sql_value(asset_no)}, {sql_value(source['location'])}, "
            f"{sql_value(source['user'])}, '{source['kind']}', '{db[asset_no]['status']}')"
        )
    statements = [
        "SET NAMES utf8mb4;",
        "CREATE TEMPORARY TABLE survey_sync (asset_no VARCHAR(255) PRIMARY KEY, "
        "location VARCHAR(255) NOT NULL, user_name VARCHAR(100), "
        "kind VARCHAR(12) NOT NULL, prior_status VARCHAR(32) NOT NULL);",
        "INSERT INTO survey_sync VALUES " + ",\n".join(values) + ";",
        "START TRANSACTION;",
        "UPDATE assets a JOIN survey_sync s ON s.asset_no = a.asset_no "
        "SET a.location = s.location, a.updated_at = NOW(6) "
        "WHERE a.status = s.prior_status AND NOT (a.location <=> s.location);",
    ]
    if resolve_unused:
        statements.append(
            "UPDATE assets a JOIN survey_sync s ON s.asset_no = a.asset_no "
            "SET a.status = 'NORMAL', a.updated_at = NOW(6) "
            "WHERE s.kind IN ('person', 'shared') AND s.prior_status = 'UNUSED' "
            "AND a.status = 'UNUSED' "
            "AND s.asset_no IN ('2600473', '2600479', '2600525', '2600526');"
        )
    status_guard = (
        "(s.prior_status = 'NORMAL' OR "
        "(s.prior_status = 'UNUSED' AND s.asset_no IN "
        "('2600473', '2600479', '2600525', '2600526')))"
        if resolve_unused else "s.prior_status = 'NORMAL'"
    )
    statements.extend([
        "UPDATE assets a JOIN survey_sync s ON s.asset_no = a.asset_no "
        "SET a.actual_user_name = s.user_name, a.actual_user_department = NULL, "
        "a.actual_user_contact = NULL, a.shared_use = b'0', a.updated_at = NOW(6) "
        f"WHERE s.kind = 'person' AND a.status = 'NORMAL' AND {status_guard} "
        "AND (NOT (a.actual_user_name <=> s.user_name) OR a.shared_use <> b'0');",
        "UPDATE assets a JOIN survey_sync s ON s.asset_no = a.asset_no "
        "SET a.actual_user_name = NULL, a.actual_user_department = NULL, "
        "a.actual_user_contact = NULL, a.shared_use = b'1', a.updated_at = NOW(6) "
        f"WHERE s.kind = 'shared' AND a.status = 'NORMAL' AND {status_guard} "
        "AND (a.actual_user_name IS NOT NULL OR a.shared_use <> b'1');",
        "UPDATE assets a JOIN survey_sync s ON s.asset_no = a.asset_no "
        "SET a.actual_user_name = NULL, a.actual_user_department = NULL, "
        "a.actual_user_contact = NULL, a.shared_use = b'0', a.updated_at = NOW(6) "
        "WHERE s.kind = 'empty' AND a.status = 'NORMAL' AND a.status = s.prior_status "
        "AND (a.actual_user_name IS NOT NULL OR a.shared_use <> b'0');",
        "COMMIT;",
    ])
    return "\n".join(statements)


def backup_database(path):
    command = [
        "docker", "exec", CONTAINER, "sh", "-ec",
        'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" '
        '--single-transaction --no-tablespaces "$MYSQL_DATABASE"',
    ]
    result = subprocess.run(command, capture_output=True)
    if result.returncode or not result.stdout.startswith(b"-- MySQL dump"):
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace"))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(result.stdout)
    return len(result.stdout)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("workbook", type=Path)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--resolve-unused", action="store_true",
                        help="Change the four known UNUSED conflicts to NORMAL")
    parser.add_argument("--backup-dir", type=Path, default=Path("outputs"))
    args = parser.parse_args()
    assets = read_workbook(args.workbook)
    db = read_db()
    before = compare(assets, db)
    print(json.dumps({"assets": len(assets), "before": before}, ensure_ascii=False))
    if not args.apply:
        return
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_path = args.backup_dir / f"asset_db_before_survey_sync_{timestamp}.sql"
    size = backup_database(backup_path)
    print(f"Backup: {backup_path.resolve()} ({size} bytes)")
    docker_mysql(build_sql(assets, db, args.resolve_unused))
    after = compare(assets, read_db())
    print(json.dumps({"after": after}, ensure_ascii=False))
    checked = ["location_mismatch", "person_mismatch", "shared_mismatch", "empty_mismatch"]
    if args.resolve_unused:
        checked += ["person_unused_conflict", "shared_unused_conflict"]
    if any(after[key] for key in checked):
        raise RuntimeError("Post-migration verification failed; inspect backup and DB")


if __name__ == "__main__":
    main()
