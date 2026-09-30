import pandas as pd
import codecs

file_path = '자 산 등 재 목 록.xls'

with open(file_path, 'rb') as f:
    header = f.read(100)
    print("File Signature:")
    print(" ".join([f"{b:02x}" for b in header[:8]]))

try:
    df = pd.read_excel(file_path, engine='xlrd')
    cols = list(df.columns)
    
    def decode_str(s):
        if not isinstance(s, str): return s
        try:
            # 엑셀에서 읽어온 문자열의 바이트를 직접 추출하여 euc-kr로 다시 디코딩 시도
            return s.encode('latin1').decode('euc-kr')
        except:
            pass
        return s

    fixed_cols = [decode_str(c) for c in cols]
    print("\nFixed Columns:")
    print(fixed_cols)
    
except Exception as e:
    print(e)