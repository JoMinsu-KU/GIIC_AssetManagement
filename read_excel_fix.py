import sys
import pandas as pd

file_path = '자 산 등 재 목 록.xls'

try:
    df = pd.read_excel(file_path, engine='xlrd')
    
    # cp1252로 디코딩 후 cp949로 재인코딩해보거나, 다른 방식으로 텍스트 복구 시도
    def fix_encoding(text):
        if not isinstance(text, str):
            return text
        try:
            return text.encode('latin1').decode('cp949')
        except:
            pass
        return text

    cols = [fix_encoding(c) for c in df.columns]
    print("\n--- Columns ---")
    print(cols)

    print("\n--- Sample Row 0 ---")
    row0 = {fix_encoding(k): fix_encoding(v) for k, v in df.iloc[0].items()}
    print(row0)

except Exception as e:
    print(f"Error: {e}")
