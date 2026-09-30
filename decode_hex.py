hex_strings = [
    "c0ac c5c5 c7a5 ba85",
    "c0c1 d0dc",
    "c790 c0b0 bc88 d638",
    "bb3c d488 ba85",
    "baa8 b378 ba85",
    "addc aca9",
    "c790 c0b0 ae08 c561",
    "cde8 b4dd c77c c790",
    "ad00 b9ac bd80 c11c",
    "be44 ce58 c7a5 c18c",
    "c2e4 c0ac c6a9 c790 ad6c bd84",
    "c2e4 c0ac c6a9 c790",
    "c2e4 c0ac c6a9 c790 ba85"
]

for hs in hex_strings:
    b = bytes.fromhex(hs)
    try:
        print(f"{hs} -> {b.decode('cp949')}")
    except Exception as e:
        print(f"{hs} -> ERR: {e}")
