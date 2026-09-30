import pandas as pd

file_path = '자 산 등 재 목 록.xlsx'

try:
    df = pd.read_excel(file_path, engine='openpyxl')
    
    # 윈도우 터미널(cmd)에서 cp949로 출력할 때 유니코드 인코딩 문제 해결을 위해
    # 문자열을 hex로 변환하거나, 인코딩을 명시적으로 처리
    def safe_print(text):
        if not isinstance(text, str): return str(text)
        try:
            return text.encode('cp949', errors='replace').decode('cp949')
        except:
            return repr(text)
            
    # 혹은 바이트 변환
    def to_hex(text):
        if not isinstance(text, str): return text
        return text.encode('utf-8').hex()
        
    cols = df.columns.tolist()
    print("--- UTF-8 Hex Columns ---")
    for c in cols:
        print(f"{to_hex(c)} -> {repr(c)}")

except Exception as e:
    print(f"Error: {e}")
