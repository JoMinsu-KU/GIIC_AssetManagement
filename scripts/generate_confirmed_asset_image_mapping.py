import csv
from collections import Counter
from pathlib import Path

from prepare_ocr_asset_match_report import write_xlsx


INPUT = Path("outputs/asset_ocr_match_report.exact_matches.csv")
OUTPUT = Path("outputs/confirmed_asset_image_mapping.xlsx")


def main() -> None:
    with INPUT.open("r", encoding="utf-8-sig", newline="") as f:
        rows = list(csv.DictReader(f))

    confirmed = [
        row
        for row in rows
        if row.get("asset_no", "").strip()
        and row.get("image_path", "").strip()
        and row.get("asset_no", "").strip() == row.get("ocr_asset_no", "").strip()
        and row.get("ocr_confidence", "").strip() == "1.0"
    ]

    counts = Counter(row["asset_no"].strip() for row in confirmed)
    output_rows = []
    for row in confirmed:
        asset_no = row["asset_no"].strip()
        output_rows.append(
            {
                "asset_no": asset_no,
                "image_path": row.get("image_path", "").strip(),
                "image_file": row.get("image_file", "").strip(),
                "asset_name": row.get("asset_name", "").strip(),
                "model": row.get("model", "").strip(),
                "excel_row": row.get("excel_row", "").strip(),
                "ocr_asset_no": row.get("ocr_asset_no", "").strip(),
                "ocr_confidence": row.get("ocr_confidence", "").strip(),
                "same_asset_image_count": str(counts[asset_no]),
            }
        )

    summary_rows = [
        {"metric": "source", "value": str(INPUT)},
        {"metric": "rule", "value": "asset_no == ocr_asset_no and ocr_confidence == 1.0"},
        {"metric": "confirmed_mapping_rows", "value": str(len(output_rows))},
        {"metric": "unique_assets", "value": str(len(counts))},
        {
            "metric": "assets_with_multiple_images",
            "value": str(sum(1 for count in counts.values() if count > 1)),
        },
    ]

    sheets = [
        ("Summary", ["metric", "value"], summary_rows),
        (
            "Confirmed Mapping",
            [
                "asset_no",
                "image_path",
                "image_file",
                "asset_name",
                "model",
                "excel_row",
                "ocr_asset_no",
                "ocr_confidence",
                "same_asset_image_count",
            ],
            output_rows,
        ),
    ]
    write_xlsx(OUTPUT, sheets)
    print(OUTPUT.resolve())
    print(f"rows={len(output_rows)} unique_assets={len(counts)}")


if __name__ == "__main__":
    main()
