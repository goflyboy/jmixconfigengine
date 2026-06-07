# -*- coding: utf-8 -*-
import codecs

# Check file encoding
with open(r'd:\03.code4cursor\jmixconfigengine\persum\本体实例化方法图解.svg', 'rb') as f:
    raw = f.read(500)
    print('First 100 bytes hex:')
    print(raw[:100].hex())
    print()

# Try different encodings
with open(r'd:\03.code4cursor\jmixconfigengine\persum\本体实例化方法图解.svg', 'rb') as f:
    raw = f.read()

print('Trying UTF-8:')
try:
    text = raw.decode('utf-8')
    # Find lines with text content
    for line in text.split('\n')[:5]:
        print(line)
except Exception as e:
    print(f'UTF-8 failed: {e}')

print()
print('Trying GBK:')
try:
    with open(r'd:\03.code4cursor\jmixconfigengine\persum\本体实例化方法图解.svg', 'r', encoding='gbk') as f:
        for line in f:
            if '本体' in line:
                print(line.strip()[:100])
                break
except Exception as e:
    print(f'GBK failed: {e}')
