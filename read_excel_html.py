import pandas as pd
import sys

file_path = '자 산 등 재 목 록.xls'

try:
    df = pd.read_html(file_path, encoding='euc-kr')
    print("Read as HTML with euc-kr")
    print(df[0].columns.tolist())
    print(df[0].iloc[0].tolist())
except Exception as e1:
    print(f"Failed to read as HTML/euc-kr: {e1}")
    try:
        df = pd.read_html(file_path, encoding='cp949')
        print("Read as HTML with cp949")
        print(df[0].columns.tolist())
        print(df[0].iloc[0].tolist())
    except Exception as e2:
        print(f"Failed to read as HTML/cp949: {e2}")

