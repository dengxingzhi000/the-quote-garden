# QuoteGarden crawler
来源: https://www.quotegarden.com/ (© 1998–2026 All rights reserved，仅抓取事实性短文本+署名，展示保留作者)
礼貌: UA `DailyMindSeed/1.0`，间隔1-1.5s，超时20s，重试3次，断点续爬 progress.json
配额: 翻译用 MyMemory 匿名（额度小），全站翻译分多天 `--limit/--offset`
运行:
python tools/crawler/quotegarden.py --self-test
python tools/crawler/quotegarden.py --limit 3
python tools/crawler/quotegarden.py --all
python tools/crawler/translate.py --limit 50
