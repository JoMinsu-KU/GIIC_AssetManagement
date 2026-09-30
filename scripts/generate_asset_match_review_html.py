import argparse
import html
import json
import re
import zipfile
from collections import defaultdict
from pathlib import Path
from urllib.parse import quote
from xml.etree import ElementTree as ET


NS_MAIN = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"
NS_REL = "{http://schemas.openxmlformats.org/package/2006/relationships}"
NS_DOC_REL = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}"


def xml_text(node):
    return "" if node is None or node.text is None else node.text


def col_index(cell_ref):
    letters = re.match(r"([A-Z]+)", cell_ref).group(1)
    value = 0
    for char in letters:
        value = value * 26 + ord(char) - ord("A") + 1
    return value


def read_shared_strings(zf):
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


def first_sheet_path(zf):
    workbook = ET.fromstring(zf.read("xl/workbook.xml"))
    first_sheet = workbook.find(f"{NS_MAIN}sheets/{NS_MAIN}sheet")
    rel_id = first_sheet.attrib[f"{NS_DOC_REL}id"]
    rels = ET.fromstring(zf.read("xl/_rels/workbook.xml.rels"))
    for rel in rels.findall(f"{NS_REL}Relationship"):
        if rel.attrib["Id"] == rel_id:
            return "xl/" + rel.attrib["Target"].lstrip("/")
    raise RuntimeError("Could not resolve first worksheet path.")


def cell_value(cell, shared_strings):
    cell_type = cell.attrib.get("t")
    if cell_type == "s":
        idx = xml_text(cell.find(f"{NS_MAIN}v"))
        return shared_strings[int(idx)] if idx else ""
    if cell_type == "inlineStr":
        return xml_text(cell.find(f"{NS_MAIN}is/{NS_MAIN}t"))
    value = xml_text(cell.find(f"{NS_MAIN}v"))
    return value[:-2] if value.endswith(".0") else value


def read_assets_from_xlsx(path):
    with zipfile.ZipFile(path) as zf:
        shared_strings = read_shared_strings(zf)
        sheet_root = ET.fromstring(zf.read(first_sheet_path(zf)))
        rows = []
        for row in sheet_root.findall(f"{NS_MAIN}sheetData/{NS_MAIN}row"):
            values = {}
            for cell in row.findall(f"{NS_MAIN}c"):
                ref = cell.attrib.get("r", "")
                if ref:
                    values[col_index(ref)] = cell_value(cell, shared_strings).strip()
            rows.append((int(row.attrib["r"]), values))

    assets = []
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


def file_url(path):
    p = Path(path)
    absolute = p.resolve()
    if absolute.drive:
        drive = absolute.drive.rstrip(":")
        rest = "/".join(quote(part) for part in absolute.parts[1:])
        return f"file:///{drive}:/{rest}"
    return absolute.as_uri()


def esc(value):
    return html.escape("" if value is None else str(value), quote=True)


def render_image_card(ocr, compact=False):
    image_url = file_url(ocr["image_path"])
    confidence = esc(ocr.get("ocr_confidence", ""))
    asset_no = esc(ocr.get("ocr_asset_no", ""))
    name = esc(ocr.get("ocr_name", ""))
    file_name = esc(ocr.get("image_file", ""))
    status = esc(ocr.get("ocr_status", ""))
    error = esc(ocr.get("error", ""))
    raw = esc(ocr.get("ocr_raw_text", ""))
    return f"""
      <figure class="image-card{' compact' if compact else ''}">
        <a href="{image_url}" target="_blank" rel="noreferrer">
          <img src="{image_url}" alt="{file_name}" loading="lazy">
        </a>
        <figcaption>
          <strong>{file_name}</strong>
          <span>OCR 자산번호: {asset_no or '-'}</span>
          <span>OCR 물품명: {name or '-'}</span>
          <span>신뢰도: {confidence or '-'}</span>
          <span>상태: {status or '-'}</span>
          {'<span class="error">오류: ' + error + '</span>' if error else ''}
          {'<details><summary>raw</summary><pre>' + raw + '</pre></details>' if raw else ''}
        </figcaption>
      </figure>
    """


def build_html(assets, ocr_results):
    assets_by_no = {asset["asset_no"]: asset for asset in assets}
    matches_by_asset = defaultdict(list)
    matched_image_paths = set()
    ocr_asset_counts = defaultdict(int)

    for ocr in ocr_results:
        ocr_no = (ocr.get("ocr_asset_no") or "").strip()
        if ocr_no:
            ocr_asset_counts[ocr_no] += 1
        if ocr.get("ocr_status") == "OK" and ocr_no in assets_by_no:
            matches_by_asset[ocr_no].append(ocr)
            matched_image_paths.add(ocr["image_path"])

    duplicate_asset_nos = {asset_no for asset_no, count in ocr_asset_counts.items() if count > 1}
    unmatched_images = [
        ocr
        for ocr in ocr_results
        if ocr.get("image_path") not in matched_image_paths
    ]

    cards = []
    for asset in assets:
        images = matches_by_asset.get(asset["asset_no"], [])
        match_count = len(images)
        duplicate = asset["asset_no"] in duplicate_asset_nos
        status = "duplicate" if duplicate else ("matched" if match_count else "unmatched")
        image_html = "".join(render_image_card(ocr) for ocr in images)
        if not image_html:
            image_html = '<div class="empty-image">매칭된 이미지 없음</div>'

        searchable = " ".join(
            [
                asset["asset_no"],
                asset["asset_name"],
                asset["model"],
                asset["serial_no"],
                asset["managing_dept"],
                asset["location"],
            ]
        ).lower()

        cards.append(
            f"""
            <article class="asset-card" data-status="{status}" data-search="{esc(searchable)}">
              <section class="asset-info">
                <div class="card-topline">
                  <span class="badge {status}">{'중복확인' if duplicate else ('매칭' if match_count else '미매칭')}</span>
                  <span class="row-ref">Excel row {esc(asset['excel_row'])}</span>
                </div>
                <h2>{esc(asset['asset_no'])}</h2>
                <dl>
                  <div><dt>물품명</dt><dd>{esc(asset['asset_name'])}</dd></div>
                  <div><dt>모델명</dt><dd>{esc(asset['model'])}</dd></div>
                  <div><dt>제조번호</dt><dd>{esc(asset['serial_no']) or '-'}</dd></div>
                  <div><dt>규격</dt><dd>{esc(asset['spec']) or '-'}</dd></div>
                  <div><dt>취득일자</dt><dd>{esc(asset['purchase_date']) or '-'}</dd></div>
                  <div><dt>관리부서</dt><dd>{esc(asset['managing_dept'])}</dd></div>
                  <div><dt>비치장소</dt><dd>{esc(asset['location'])}</dd></div>
                </dl>
              </section>
              <section class="asset-images">
                {image_html}
              </section>
            </article>
            """
        )

    unmatched_html = "".join(render_image_card(ocr, compact=True) for ocr in unmatched_images)

    summary = {
        "assets": len(assets),
        "images": len(ocr_results),
        "matched_assets": sum(1 for asset in assets if matches_by_asset.get(asset["asset_no"])),
        "matched_images": len(matched_image_paths),
        "unmatched_images": len(unmatched_images),
        "duplicate_assets": len(duplicate_asset_nos),
    }

    return f"""<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>자산 OCR 매칭 검수</title>
  <style>
    :root {{
      --bg: #f5f7fb;
      --panel: #ffffff;
      --line: #dfe6f1;
      --text: #182235;
      --muted: #6c7a90;
      --blue: #315fce;
      --green: #16845f;
      --red: #c13c3c;
      --amber: #9a6400;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      background: var(--bg);
      color: var(--text);
      font-family: "Segoe UI", "Malgun Gothic", Arial, sans-serif;
    }}
    header {{
      position: sticky;
      top: 0;
      z-index: 10;
      padding: 18px 24px;
      border-bottom: 1px solid var(--line);
      background: rgba(255,255,255,.96);
      backdrop-filter: blur(8px);
    }}
    h1 {{ margin: 0 0 14px; font-size: 24px; }}
    .summary {{
      display: grid;
      grid-template-columns: repeat(6, minmax(120px, 1fr));
      gap: 10px;
      margin-bottom: 14px;
    }}
    .metric {{
      padding: 10px 12px;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: #fbfcff;
    }}
    .metric strong {{ display: block; font-size: 20px; }}
    .metric span {{ color: var(--muted); font-size: 12px; }}
    .controls {{
      display: flex;
      gap: 10px;
      align-items: center;
      flex-wrap: wrap;
    }}
    input, select, button {{
      min-height: 38px;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: #fff;
      color: var(--text);
      font: inherit;
      padding: 0 12px;
    }}
    input {{ min-width: 320px; flex: 1; }}
    button {{ cursor: pointer; font-weight: 700; }}
    main {{ padding: 20px 24px 48px; }}
    .asset-list {{ display: grid; gap: 14px; }}
    .asset-card {{
      display: grid;
      grid-template-columns: minmax(360px, .85fr) minmax(420px, 1.15fr);
      gap: 18px;
      padding: 18px;
      border: 1px solid var(--line);
      border-radius: 10px;
      background: var(--panel);
    }}
    .card-topline {{
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: center;
      margin-bottom: 8px;
    }}
    .badge {{
      display: inline-flex;
      align-items: center;
      min-height: 24px;
      padding: 0 9px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 800;
    }}
    .badge.matched {{ color: var(--green); background: #e8f7f0; }}
    .badge.unmatched {{ color: var(--red); background: #fdecec; }}
    .badge.duplicate {{ color: var(--amber); background: #fff3d8; }}
    .row-ref {{ color: var(--muted); font-size: 12px; }}
    h2 {{ margin: 0 0 12px; font-size: 22px; }}
    dl {{ display: grid; gap: 8px; margin: 0; }}
    dl div {{ display: grid; grid-template-columns: 82px 1fr; gap: 10px; }}
    dt {{ color: var(--muted); font-size: 13px; }}
    dd {{ margin: 0; word-break: keep-all; overflow-wrap: anywhere; }}
    .asset-images {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 12px;
      align-content: start;
    }}
    .image-card {{
      margin: 0;
      border: 1px solid var(--line);
      border-radius: 8px;
      overflow: hidden;
      background: #fbfcff;
    }}
    .image-card img {{
      display: block;
      width: 100%;
      height: 220px;
      object-fit: contain;
      background: #eef2f8;
    }}
    .image-card.compact img {{ height: 180px; }}
    figcaption {{
      display: grid;
      gap: 5px;
      padding: 10px;
      font-size: 13px;
    }}
    figcaption span {{ color: var(--muted); }}
    figcaption strong {{ overflow-wrap: anywhere; }}
    details summary {{ cursor: pointer; color: var(--blue); }}
    pre {{
      max-height: 140px;
      overflow: auto;
      white-space: pre-wrap;
      font-size: 12px;
      color: #344156;
    }}
    .error {{ color: var(--red); }}
    .empty-image {{
      min-height: 180px;
      display: grid;
      place-items: center;
      color: var(--muted);
      border: 1px dashed var(--line);
      border-radius: 8px;
      background: #fbfcff;
    }}
    .unmatched-section {{
      margin-top: 30px;
      padding-top: 22px;
      border-top: 1px solid var(--line);
    }}
    .unmatched-grid {{
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 12px;
    }}
    .hidden {{ display: none !important; }}
    @media (max-width: 900px) {{
      header {{ position: static; }}
      .summary {{ grid-template-columns: repeat(2, 1fr); }}
      .asset-card {{ grid-template-columns: 1fr; }}
      input {{ min-width: 100%; }}
    }}
  </style>
</head>
<body>
  <header>
    <h1>자산 OCR 매칭 검수</h1>
    <section class="summary">
      <div class="metric"><strong>{summary['assets']}</strong><span>엑셀 자산</span></div>
      <div class="metric"><strong>{summary['images']}</strong><span>OCR 이미지</span></div>
      <div class="metric"><strong>{summary['matched_assets']}</strong><span>매칭 자산</span></div>
      <div class="metric"><strong>{summary['matched_images']}</strong><span>매칭 이미지</span></div>
      <div class="metric"><strong>{summary['unmatched_images']}</strong><span>미매칭 이미지</span></div>
      <div class="metric"><strong>{summary['duplicate_assets']}</strong><span>중복 의심</span></div>
    </section>
    <div class="controls">
      <input id="searchInput" type="search" placeholder="자산번호, 물품명, 모델명, 부서, 장소 검색">
      <select id="statusFilter">
        <option value="all">전체 자산</option>
        <option value="matched">매칭만</option>
        <option value="unmatched">미매칭 자산만</option>
        <option value="duplicate">중복 의심만</option>
      </select>
      <button id="resetButton" type="button">초기화</button>
    </div>
  </header>
  <main>
    <section class="asset-list" id="assetList">
      {''.join(cards)}
    </section>
    <section class="unmatched-section">
      <h2>미매칭/OCR 실패 이미지</h2>
      <div class="unmatched-grid">
        {unmatched_html}
      </div>
    </section>
  </main>
  <script>
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');
    const resetButton = document.getElementById('resetButton');
    const cards = Array.from(document.querySelectorAll('.asset-card'));

    function applyFilters() {{
      const query = searchInput.value.trim().toLowerCase();
      const status = statusFilter.value;
      for (const card of cards) {{
        const matchesQuery = !query || card.dataset.search.includes(query);
        const matchesStatus = status === 'all' || card.dataset.status === status;
        card.classList.toggle('hidden', !(matchesQuery && matchesStatus));
      }}
    }}

    searchInput.addEventListener('input', applyFilters);
    statusFilter.addEventListener('change', applyFilters);
    resetButton.addEventListener('click', () => {{
      searchInput.value = '';
      statusFilter.value = 'all';
      applyFilters();
    }});
  </script>
</body>
</html>
"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--excel", required=True)
    parser.add_argument("--cache", default="outputs/asset_ocr_cache.json")
    parser.add_argument("--output", default="outputs/asset_ocr_review.html")
    args = parser.parse_args()

    assets = read_assets_from_xlsx(Path(args.excel))
    ocr_results = json.loads(Path(args.cache).read_text(encoding="utf-8"))
    html_text = build_html(assets, ocr_results)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(html_text, encoding="utf-8")
    print(output.resolve())


if __name__ == "__main__":
    main()
