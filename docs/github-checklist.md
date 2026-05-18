# GitHub Release Checklist

## 1. Before Commit

确认不要提交：

- `.env`
- `target/`
- `.idea/`
- `uploads/`
- 本地日志文件

已经通过 `.gitignore` 忽略。

## 2. Sensitive Information

上传前搜索：

```bash
grep -R "password\\|123321\\|abc123456\\|Kuanfan" .
```

Windows PowerShell:

```powershell
Select-String -Path README.md,docs\\*.md,src\\main\\resources\\application.yaml,src\\main\\java\\com\\hmdp\\**\\*.java -Pattern "123321|abc123456|Kuanfan|183\\.169|E:\\\\hmdping"
```

预期：没有结果。

## 3. Recommended Commit Structure

建议分 4 个 commit：

```text
feat: stabilize feed and seckill base flow
feat: add kafka async voucher order pipeline
feat: add elasticsearch shop search and agent tool APIs
docs: add architecture, deployment, and interview guide
```

## 4. Repository Description

GitHub description 建议：

```text
Local life backend platform with Redis cache governance, Kafka seckill pipeline, Elasticsearch search, and Agent tool orchestration.
```

Topics 建议：

```text
spring-boot redis kafka elasticsearch redisson lua fastapi agent
```

## 5. Interview Demo Script

演示顺序：

1. 打开 README，讲项目定位和架构图。
2. 打开 `docs/architecture.md`，讲缓存、秒杀、搜索、Agent 四条主线。
3. 打开 `VoucherOrderServiceImpl`，讲 Lua + Kafka + Redisson + MySQL 兜底。
4. 打开 `CacheClient`，讲缓存治理封装。
5. 打开 `ShopServiceImpl.search`，讲 ES 搜索。
6. 打开 `agent-service/main.py`，讲 Agent 如何调用 Java 工具接口。

## 6. Known Current Limits

当前版本为了简历展示和学习清晰，做了这些取舍：

- ES 同步是手动接口，不是 Canal 或 CDC 自动同步。
- Kafka 失败处理依赖日志和消费重试，还未加死信队列。
- Agent 服务当前是规则提取关键词的骨架，后续可接 LangChain Tool Calling。
- 前端页面不是本次重点，后端接口和架构说明优先。
