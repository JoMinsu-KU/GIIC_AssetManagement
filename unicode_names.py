import unicodedata

hex_strings = [
    "ec82acec9785ec9ea5ebaa85",
    "ec8381ed839c",
    "ec9e90ec82b0ebb288ed98b8",
    "ebacbced9288ebaa85",
    "ebaaa8eb8db8ebaa85",
    "eab79ceab2a9",
    "ec9e90ec82b0eab888ec95a1",
    "ecb7a8eb939dec9dbcec9e90",
    "eab480eba6acebb680ec849c",
    "ebb984ecb998ec9ea5ec868c",
    "ec8ba4ec82acec9aa9ec9e90eab5acebb684",
    "ec8ba4ec82acec9aa9ec9e90",
    "ec8ba4ec82acec9aa9ec9e90ebaa85"
]

for hs in hex_strings:
    b = bytes.fromhex(hs)
    decoded = b.decode('utf-8')
    names = []
    for char in decoded:
        try:
            names.append(unicodedata.name(char))
        except ValueError:
            names.append("UNKNOWN")
    print(f"{hs} -> {', '.join(names)}")
