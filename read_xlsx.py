import pandas as pd

file_path = '자 산 등 재 목 록.xlsx'

try:
    df = pd.read_excel(file_path, engine='openpyxl')
    
    print("\n--- Columns ---")
    cols = list(df.columns)
    print(cols)
    
    print("\n--- Sample Row 0 ---")
    print(df.iloc[0].to_dict())
    
except Exception as e:
    print(f"Error: {e}")
