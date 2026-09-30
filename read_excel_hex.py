import xlrd

file_path = '자 산 등 재 목 록.xls'

workbook = xlrd.open_workbook(file_path, encoding_override="cp949")
sheet = workbook.sheet_by_index(0)

headers = sheet.row_values(0)
row1 = sheet.row_values(1)

def as_hex(text):
    if not isinstance(text, str): return text
    return " ".join([f"{ord(c):04x}" for c in text])

print("Headers (Hex):")
for h in headers:
    print(f"{h} -> {as_hex(h)}")
