import pandas as pd
import traceback
import sys

file_path = '자 산 등 재 목 록.xls'

try:
    print(f"Attempting to read {file_path}")
    
    try:
        df = pd.read_excel(file_path, engine='xlrd')
        print("Success: read_excel (xlrd)")
    except Exception as e:
        print(f"Failed xlrd: {e}")
        try:
            df = pd.read_excel(file_path, engine='openpyxl')
            print("Success: read_excel (openpyxl)")
        except Exception as e:
            print(f"Failed openpyxl: {e}")
            try:
                # Many ERP/web systems export "xls" that is actually HTML
                dfs = pd.read_html(file_path, encoding='utf-8')
                df = dfs[0]
                print("Success: read_html (utf-8)")
            except Exception as e:
                print(f"Failed read_html (utf-8): {e}")
                try:
                    dfs = pd.read_html(file_path, encoding='cp949')
                    df = dfs[0]
                    print("Success: read_html (cp949)")
                except Exception as e:
                    print(f"Failed read_html (cp949): {e}")
                    try:
                        # Sometimes it's actually CSV disguised as XLS
                        df = pd.read_csv(file_path, sep='\t', encoding='cp949')
                        print("Success: read_csv (tab-separated, cp949)")
                    except Exception as e:
                        print(f"Failed read_csv (tab-separated, cp949): {e}")
                        sys.exit(1)

    print("\n" + "="*50)
    print("DATAFRAME INFO:")
    print("="*50)
    df.info()
    
    print("\n" + "="*50)
    print("COLUMN HEADERS:")
    print("="*50)
    for col in df.columns:
        print(f" - {col}")
        
    print("\n" + "="*50)
    print("SAMPLE DATA (First 3 rows):")
    print("="*50)
    # Using to_string to prevent pandas from truncating output
    print(df.head(3).to_string())

except Exception as e:
    print(f"Unhandled error: {e}")
    traceback.print_exc()
