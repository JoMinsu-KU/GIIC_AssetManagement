import argparse
import base64
import csv
import datetime as dt
import hashlib
import hmac
import json
import mimetypes
import os
import re
import time
import urllib.error
import urllib.request
import uuid
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET


NS_MAIN = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"
NS_REL = "{http://schemas.openxmlformats.org/package/2006/relationships}"
NS_DOC_REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}"


def read_dotenv_value(path: Path, name: str) -> str | None:
    if not path.exists():
        return None
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        if key.strip() == name:
            return value.strip().strip('"').strip("'")
    return None


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("ascii").rstrip("=")


def create_access_token(secret: str, employee_no: str = "2025009", role: str = "SUPER") -> str:
    now = int(time.time())
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "sub": employee_no,
        "role": role,
        "token_type": "access",
        "iat": now,
        "exp": now + 86400,
    }
    unsigned = ".".join(
        [
            b64url(json.dumps(header, separators=(",", ":")).encode("utf-8")),
            b64url(json.dumps(payload, separators=(",", ":")).encode("utf-8")),
        ]
    )
    signature = hmac.new(secret.encode("utf-8"), unsigned.encode("ascii"), hashlib.sha256).digest()
    return f"{unsigned}.{b64url(signature)}"


def xml_text(node: ET.Element | None) -> str:
    return "" if node is None or node.text is None else node.text


def col_index(cell_ref: str) -> int:
    letters = re.match(r"([A-Z]+)", cell_ref).group(1)
    value = 0
    for char in letters:
        value = value * 26 + ord(char) - ord("A") + 1
    return value


def read_shared_strings(zf: zipfile.ZipFile) -> list[str]:
    try:
        root = ET.fromstring(zf.read("xl/sharedStrings.xml"))
    except KeyError:
        return []
    values = []
    for si in root.findall(f"{NS_MAIN}si"):
        pieces = []
        direct = si.find(f"{NS_MAIN}t")
        if direct is not None:
            pieces.append(xml_text(direct))
        for run in si.findall(f"{NS_MAIN}r"):
            pieces.append(xml_text(run.find(f"{NS_MAIN}t")))
        values.append("".join(pieces))
    return values


def first_sheet_path(zf: zipfile.ZipFile) -> str:
    workbook = ET.fromstring(zf.read("xl/workbook.xml"))
    first_sheet = workbook.find(f"{NS_MAIN}sheets/{NS_MAIN}sheet")
    if first_sheet is None:
        raise RuntimeError("No sheets found in workbook.")
    rel_id = first_sheet.attrib[f"{NS_DOC_REL}id"]

    rels = ET.fromstring(zf.read("xl/_rels/workbook.xml.rels"))
    for rel in rels.findall(f"{NS_REL}Relationship"):
        if rel.attrib["Id"] == rel_id:
            target = rel.attrib["Target"]
            return "xl/" + target.lstrip("/")
    raise RuntimeError("Could not resolve first worksheet path.")


def cell_value(cell: ET.Element, shared_strings: list[str]) -> str:
    cell_type = cell.attrib.get("t")
    if cell_type == "s":
        idx_text = xml_text(cell.find(f"{NS_MAIN}v"))
        return shared_strings[int(idx_text)] if idx_text else ""
    if cell_type == "inlineStr":
        return xml_text(cell.find(f"{NS_MAIN}is/{NS_MAIN}t"))
    value = xml_text(cell.find(f"{NS_MAIN}v"))
    if value.endswith(".0"):
        value = value[:-2]
    return value


def read_assets_from_xlsx(path: Path) -> list[dict[str, str]]:
    with zipfile.ZipFile(path) as zf:
        shared_strings = read_shared_strings(zf)
        sheet_root = ET.fromstring(zf.read(first_sheet_path(zf)))

        rows = []
        for row in sheet_root.findall(f"{NS_MAIN}sheetData/{NS_MAIN}row"):
            values = {}
            for cell in row.findall(f"{NS_MAIN}c"):
                ref = cell.attrib.get("r", "")
                if not ref:
                    continue
                values[col_index(ref)] = cell_value(cell, shared_strings).strip()
            rows.append((int(row.attrib["r"]), values))

    assets = []
    # Known columns from the source workbook:
    # C status, D asset no, F item name, G model, H serial, I spec, J value,
    # K purchase date, M managing dept, N location.
    for row_number, values in rows[1:]:
        asset_no = values.get(4, "").strip()
        if not asset_no:
            continue
        assets.append(
            {
                "excel_row": str(row_number),
                "asset_no": asset_no,
                "status": values.get(3, ""),
                "asset_name": values.get(6, ""),
                "model": values.get(7, ""),
                "serial_no": values.get(8, ""),
                "spec": values.get(9, ""),
                "value": values.get(10, ""),
                "purchase_date": values.get(11, ""),
                "managing_dept": values.get(13, ""),
                "location": values.get(14, ""),
            }
        )
    return assets


def post_ocr(api_base_url: str, token: str, image_path: Path) -> dict:
    boundary = "----CodexBoundary" + uuid.uuid4().hex
    mime_type = mimetypes.guess_type(image_path.name)[0] or "image/jpeg"
    file_bytes = image_path.read_bytes()
    body = b"".join(
        [
            f"--{boundary}\r\n".encode("utf-8"),
            (
                f'Content-Disposition: form-data; name="file"; filename="{image_path.name}"\r\n'
                f"Content-Type: {mime_type}\r\n\r\n"
            ).encode("utf-8"),
            file_bytes,
            b"\r\n",
            f"--{boundary}--\r\n".encode("utf-8"),
        ]
    )
    request = urllib.request.Request(
        f"{api_base_url.rstrip('/')}/assets/ocr",
        data=body,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=140) as response:
        payload = json.loads(response.read().decode("utf-8"))
    return payload.get("data") or {}


def load_cache(path: Path) -> dict[str, dict]:
    if not path.exists():
        return {}
    items = json.loads(path.read_text(encoding="utf-8"))
    return {item["image_path"]: item for item in items}


def save_cache(path: Path, cache: dict[str, dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    rows = sorted(cache.values(), key=lambda item: item.get("image_file", ""))
    path.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")


def ocr_images(image_dir: Path, api_base_url: str, token: str, cache_path: Path) -> list[dict]:
    cache = load_cache(cache_path)
    image_paths = sorted(
        [
            p
            for p in image_dir.iterdir()
            if p.is_file() and p.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}
        ],
        key=lambda p: p.name,
    )

    results = []
    for index, path in enumerate(image_paths, start=1):
        key = str(path)
        if key in cache:
            print(f"[{index}/{len(image_paths)}] cached {path.name}", flush=True)
            results.append(cache[key])
            continue

        print(f"[{index}/{len(image_paths)}] OCR {path.name}", flush=True)
        row = {
            "image_file": path.name,
            "image_path": str(path),
            "file_size": str(path.stat().st_size),
            "last_write_time": dt.datetime.fromtimestamp(path.stat().st_mtime).strftime("%Y-%m-%d %H:%M:%S"),
            "ocr_status": "OK",
            "ocr_asset_no": "",
            "ocr_name": "",
            "ocr_managing_dept": "",
            "ocr_purchase_date": "",
            "ocr_spec": "",
            "ocr_confidence": "",
            "error": "",
            "ocr_raw_text": "",
        }
        try:
            data = post_ocr(api_base_url, token, path)
            row["ocr_asset_no"] = str(data.get("assetNo") or "").strip()
            row["ocr_name"] = str(data.get("name") or "")
            row["ocr_managing_dept"] = str(data.get("managingDept") or "")
            row["ocr_purchase_date"] = str(data.get("purchaseDate") or "")
            row["ocr_spec"] = str(data.get("spec") or "")
            row["ocr_confidence"] = str(data.get("confidence") or "")
            row["ocr_raw_text"] = str(data.get("rawText") or "")
        except urllib.error.HTTPError as exc:
            row["ocr_status"] = "ERROR"
            row["error"] = f"HTTP {exc.code}: {exc.read().decode('utf-8', errors='replace')}"
        except Exception as exc:
            row["ocr_status"] = "ERROR"
            row["error"] = str(exc)

        cache[key] = row
        results.append(row)
        save_cache(cache_path, cache)
        time.sleep(0.35)

    return results


def xml_escape(value: object) -> str:
    text = "" if value is None else str(value)
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def col_name(index: int) -> str:
    name = ""
    while index:
        index, rem = divmod(index - 1, 26)
        name = chr(ord("A") + rem) + name
    return name


def sheet_xml(headers: list[str], rows: list[dict[str, object]]) -> str:
    lines = [
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">',
        "<sheetData>",
    ]
    all_rows = [dict(zip(headers, headers))] + rows
    for row_idx, row in enumerate(all_rows, start=1):
        lines.append(f'<row r="{row_idx}">')
        for col_idx, header in enumerate(headers, start=1):
            ref = f"{col_name(col_idx)}{row_idx}"
            value = xml_escape(row.get(header, ""))
            lines.append(f'<c r="{ref}" t="inlineStr"><is><t>{value}</t></is></c>')
        lines.append("</row>")
    lines.extend(["</sheetData>", "</worksheet>"])
    return "\n".join(lines)


def write_xlsx(output_path: Path, sheets: list[tuple[str, list[str], list[dict[str, object]]]]) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    content_types = [
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">',
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>',
        '<Default Extension="xml" ContentType="application/xml"/>',
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>',
        '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>',
    ]
    for idx in range(1, len(sheets) + 1):
        content_types.append(
            f'<Override PartName="/xl/worksheets/sheet{idx}.xml" '
            'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
        )
    content_types.append("</Types>")

    workbook_sheets = []
    workbook_rels = [
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">',
    ]
    for idx, (name, _, _) in enumerate(sheets, start=1):
        workbook_sheets.append(
            f'<sheet name="{xml_escape(name[:31])}" sheetId="{idx}" '
            f'r:id="rId{idx}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>'
        )
        workbook_rels.append(
            f'<Relationship Id="rId{idx}" '
            'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" '
            f'Target="worksheets/sheet{idx}.xml"/>'
        )
    workbook_rels.append(
        f'<Relationship Id="rId{len(sheets) + 1}" '
        'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" '
        'Target="styles.xml"/>'
    )
    workbook_rels.append("</Relationships>")

    workbook_xml = "\n".join(
        [
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
            '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">',
            "<sheets>",
            *workbook_sheets,
            "</sheets>",
            "</workbook>",
        ]
    )

    styles_xml = "\n".join(
        [
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
            '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">',
            '<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>',
            '<fills count="1"><fill><patternFill patternType="none"/></fill></fills>',
            '<borders count="1"><border/></borders>',
            '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>',
            '<cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>',
            "</styleSheet>",
        ]
    )

    with zipfile.ZipFile(output_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("[Content_Types].xml", "\n".join(content_types))
        zf.writestr(
            "_rels/.rels",
            "\n".join(
                [
                    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
                    '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">',
                    '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>',
                    "</Relationships>",
                ]
            ),
        )
        zf.writestr("xl/workbook.xml", workbook_xml)
        zf.writestr("xl/_rels/workbook.xml.rels", "\n".join(workbook_rels))
        zf.writestr("xl/styles.xml", styles_xml)
        for idx, (_, headers, rows) in enumerate(sheets, start=1):
            zf.writestr(f"xl/worksheets/sheet{idx}.xml", sheet_xml(headers, rows))


def write_csv(path: Path, headers: list[str], rows: list[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=headers)
        writer.writeheader()
        writer.writerows(rows)


def build_report(assets: list[dict], ocr_results: list[dict]) -> tuple[list[tuple[str, list[str], list[dict]]], dict]:
    assets_by_no: dict[str, list[dict]] = {}
    for asset in assets:
        assets_by_no.setdefault(asset["asset_no"].strip(), []).append(asset)

    exact_matches = []
    unmatched = []
    duplicate_map: dict[str, list[str]] = {}
    for ocr in ocr_results:
        ocr_asset_no = ocr.get("ocr_asset_no", "").strip()
        if ocr.get("ocr_status") != "OK" or not ocr_asset_no or ocr_asset_no not in assets_by_no:
            unmatched.append(
                {
                    "image_file": ocr.get("image_file", ""),
                    "image_path": ocr.get("image_path", ""),
                    "ocr_status": ocr.get("ocr_status", ""),
                    "ocr_asset_no": ocr.get("ocr_asset_no", ""),
                    "ocr_name": ocr.get("ocr_name", ""),
                    "ocr_confidence": ocr.get("ocr_confidence", ""),
                    "error": ocr.get("error", ""),
                }
            )
            continue

        duplicate_map.setdefault(ocr_asset_no, []).append(ocr.get("image_file", ""))
        for asset in assets_by_no[ocr_asset_no]:
            exact_matches.append(
                {
                    "excel_row": asset["excel_row"],
                    "asset_no": asset["asset_no"],
                    "asset_name": asset["asset_name"],
                    "model": asset["model"],
                    "serial_no": asset["serial_no"],
                    "managing_dept": asset["managing_dept"],
                    "location": asset["location"],
                    "image_file": ocr.get("image_file", ""),
                    "image_path": ocr.get("image_path", ""),
                    "ocr_asset_no": ocr.get("ocr_asset_no", ""),
                    "ocr_name": ocr.get("ocr_name", ""),
                    "ocr_confidence": ocr.get("ocr_confidence", ""),
                }
            )

    duplicate_rows = [
        {"asset_no": key, "image_count": str(len(files)), "image_files": ", ".join(files)}
        for key, files in sorted(duplicate_map.items())
        if len(files) > 1
    ]

    matches_by_asset = {}
    for match in exact_matches:
        matches_by_asset.setdefault(match["asset_no"], []).append(match["image_file"])
    asset_status = [
        {
            "excel_row": asset["excel_row"],
            "asset_no": asset["asset_no"],
            "asset_name": asset["asset_name"],
            "model": asset["model"],
            "managing_dept": asset["managing_dept"],
            "location": asset["location"],
            "match_count": str(len(matches_by_asset.get(asset["asset_no"], []))),
            "matched_images": ", ".join(matches_by_asset.get(asset["asset_no"], [])),
        }
        for asset in assets
    ]

    summary = [
        {"metric": "Excel assets", "value": str(len(assets))},
        {"metric": "Images processed", "value": str(len(ocr_results))},
        {"metric": "Exact match rows", "value": str(len(exact_matches))},
        {"metric": "Unmatched/error images", "value": str(len(unmatched))},
        {"metric": "Duplicate OCR asset numbers", "value": str(len(duplicate_rows))},
        {
            "metric": "Match rule",
            "value": "OCR asset number exactly equals Excel asset number after trimming whitespace.",
        },
    ]

    sheets = [
        ("Summary", ["metric", "value"], summary),
        (
            "Exact Matches",
            [
                "excel_row",
                "asset_no",
                "asset_name",
                "model",
                "serial_no",
                "managing_dept",
                "location",
                "image_file",
                "image_path",
                "ocr_asset_no",
                "ocr_name",
                "ocr_confidence",
            ],
            exact_matches,
        ),
        (
            "Asset Match Status",
            [
                "excel_row",
                "asset_no",
                "asset_name",
                "model",
                "managing_dept",
                "location",
                "match_count",
                "matched_images",
            ],
            asset_status,
        ),
        (
            "Unmatched OCR",
            ["image_file", "image_path", "ocr_status", "ocr_asset_no", "ocr_name", "ocr_confidence", "error"],
            unmatched,
        ),
        ("Duplicate OCR AssetNo", ["asset_no", "image_count", "image_files"], duplicate_rows),
        (
            "All OCR Results",
            [
                "image_file",
                "image_path",
                "file_size",
                "last_write_time",
                "ocr_status",
                "ocr_asset_no",
                "ocr_name",
                "ocr_managing_dept",
                "ocr_purchase_date",
                "ocr_spec",
                "ocr_confidence",
                "error",
                "ocr_raw_text",
            ],
            ocr_results,
        ),
    ]
    stats = {
        "assets": len(assets),
        "ocr_results": len(ocr_results),
        "exact_matches": len(exact_matches),
        "unmatched": len(unmatched),
        "duplicates": len(duplicate_rows),
    }
    return sheets, stats


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--excel", required=True)
    parser.add_argument("--images", required=True)
    parser.add_argument("--api-base-url", default="http://localhost:8080/api")
    parser.add_argument("--output", default="outputs/asset_ocr_match_report.xlsx")
    parser.add_argument("--cache", default="outputs/asset_ocr_cache.json")
    args = parser.parse_args()

    root = Path.cwd()
    jwt_secret = read_dotenv_value(root / ".env", "JWT_SECRET") or os.environ.get("JWT_SECRET")
    if not jwt_secret:
        raise RuntimeError("JWT_SECRET was not found in .env or environment variables.")

    excel_path = Path(args.excel)
    image_dir = Path(args.images)
    output_path = Path(args.output)
    cache_path = Path(args.cache)

    print("Reading Excel assets...", flush=True)
    assets = read_assets_from_xlsx(excel_path)
    print(f"Excel assets: {len(assets)}", flush=True)

    token = create_access_token(jwt_secret)
    ocr_results = ocr_images(image_dir, args.api_base_url, token, cache_path)
    sheets, stats = build_report(assets, ocr_results)

    write_xlsx(output_path, sheets)
    write_csv(output_path.with_suffix(".exact_matches.csv"), sheets[1][1], sheets[1][2])
    print(f"Report saved: {output_path}", flush=True)
    print(f"Exact match CSV saved: {output_path.with_suffix('.exact_matches.csv')}", flush=True)
    print(json.dumps(stats, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    main()
