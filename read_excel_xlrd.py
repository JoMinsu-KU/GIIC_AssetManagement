import xlrd

file_path = '자 산 등 재 목 록.xls'

# xlrd로 직접 열어보기
workbook = xlrd.open_workbook(file_path, encoding_override="cp949")
sheet = workbook.sheet_by_index(0)

# 첫 번째 행 (헤더) 출력
print("Headers:")
row_vals = sheet.row_values(0)
print(row_vals)

print("\nRow 1:")
print(sheet.row_values(1))
