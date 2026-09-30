import xlrd

file_path = '자 산 등 재 목 록.xls'

# cp1252로 읽혀서 깨진 것일 수 있으므로 latin1 -> cp949 변환 시도
def fix_text(text):
    if not isinstance(text, str):
        return text
    try:
        return text.encode('latin1').decode('cp949')
    except Exception as e:
        return f"{text} (ERR: {e})"

workbook = xlrd.open_workbook(file_path)
sheet = workbook.sheet_by_index(0)

headers = sheet.row_values(0)
row1 = sheet.row_values(1)

print("Headers (Fixed):")
print([fix_text(h) for h in headers])

print("\nRow 1 (Fixed):")
print([fix_text(r) for r in row1])
