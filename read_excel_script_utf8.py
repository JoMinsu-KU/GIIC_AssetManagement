import sys

try:
    import pandas as pd
except ImportError:
    print("pandas not installed")
    sys.exit(1)

file_path = '자 산 등 재 목 록.xls'

try:
    df = pd.read_excel(file_path, engine='xlrd')
    
    print("\n--- Columns ---")
    for idx, col in enumerate(df.columns):
        print(f"{idx}: {col}")
        
    print("\n--- Sample Row 0 ---")
    for col in df.columns:
        print(f"{col}: {df.iloc[0][col]}")
        
    print("\n--- Sample Row 1 ---")
    for col in df.columns:
        print(f"{col}: {df.iloc[1][col]}")
        
except Exception as e:
    print(f"Error: {e}")
