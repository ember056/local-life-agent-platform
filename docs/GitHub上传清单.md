# GitHub Release Checklist

## 1. Before Commit

纭涓嶈鎻愪氦锛?
- `.env`
- `target/`
- `.idea/`
- `uploads/`
- 鏈湴鏃ュ織鏂囦欢

宸茬粡閫氳繃 `.gitignore` 蹇界暐銆?
## 2. Sensitive Information

涓婁紶鍓嶆悳绱細

```bash
grep -R "password\\|123321\\|abc123456\\|Kuanfan" .
```

Windows PowerShell:

```powershell
Select-String -Path README.md,docs\\*.md,src\\main\\resources\\application.yaml,src\\main\\java\\com\\hmdp\\**\\*.java -Pattern "123321|abc123456|Kuanfan|183\\.169|E:\\\\hmdping"
```

棰勬湡锛氭病鏈夌粨鏋溿€?
## 3. Recommended Commit Structure

寤鸿鍒?4 涓?commit锛?
```text
feat: stabilize feed and seckill base flow
feat: add kafka async voucher order pipeline
feat: add elasticsearch shop search and agent tool APIs
docs: add architecture, deployment, and interview guide
```

## 4. Repository Description

GitHub description 寤鸿锛?
```text
Local life backend platform with Redis cache governance, Kafka seckill pipeline, Elasticsearch search, and Agent tool orchestration.
```

Topics 寤鸿锛?
```text
spring-boot redis kafka elasticsearch redisson lua fastapi agent
```

## 5. Interview Demo Script

婕旂ず椤哄簭锛?
1. 鎵撳紑 README锛岃椤圭洰瀹氫綅鍜屾灦鏋勫浘銆?2. 鎵撳紑 `docs/架构设计.md`锛岃缂撳瓨銆佺鏉€銆佹悳绱€丄gent 鍥涙潯涓荤嚎銆?3. 鎵撳紑 `VoucherOrderServiceImpl`锛岃 Lua + Kafka + Redisson + MySQL 鍏滃簳銆?4. 鎵撳紑 `CacheClient`锛岃缂撳瓨娌荤悊灏佽銆?5. 鎵撳紑 `ShopServiceImpl.search`锛岃 ES 鎼滅储銆?6. 鎵撳紑 `agent-service/main.py`锛岃 Agent 濡備綍璋冪敤 Java 宸ュ叿鎺ュ彛銆?
## 6. Known Current Limits

褰撳墠鐗堟湰涓轰簡绠€鍘嗗睍绀哄拰瀛︿範娓呮櫚锛屽仛浜嗚繖浜涘彇鑸嶏細

- ES 鍚屾鏄墜鍔ㄦ帴鍙ｏ紝涓嶆槸 Canal 鎴?CDC 鑷姩鍚屾銆?- Kafka 澶辫触澶勭悊渚濊禆鏃ュ織鍜屾秷璐归噸璇曪紝杩樻湭鍔犳淇￠槦鍒椼€?- Agent 鏈嶅姟褰撳墠鏄鍒欐彁鍙栧叧閿瘝鐨勯鏋讹紝鍚庣画鍙帴 LangChain Tool Calling銆?- 鍓嶇椤甸潰涓嶆槸鏈閲嶇偣锛屽悗绔帴鍙ｅ拰鏋舵瀯璇存槑浼樺厛銆?
