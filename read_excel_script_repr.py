import sys
import pandas as pd

file_path = '자 산 등 재 목 록.xls'

try:
    df = pd.read_excel(file_path, engine='xlrd')
    
    print("\n--- Columns ---")
    cols = list(df.columns)
    # 컬럼 이름을 cp949로 인코딩 후 디코딩해보거나, 다른 인코딩 시도
    # xlrd가 내부적으로 유니코드로 읽었을 테지만, 원본이 깨져있을 수 있음
    for idx, col in enumerate(cols):
        # 만약 col이 제대로 출력되지 않는다면 그냥 배열 형태로 출력
        pass
        
    # 그냥 리스트의 repr() 출력하여 안전하게 보기
    print(repr(cols))
    
    print("\n--- Sample Row 0 ---")
    row0 = df.iloc[0].to_dict()
    print(repr(row0))
    
    print("\n--- Sample Row 1 ---")
    row1 = df.iloc[1].to_dict()
    print(repr(row1))
        
except Exception as e:
    print(f"Error: {e}")
