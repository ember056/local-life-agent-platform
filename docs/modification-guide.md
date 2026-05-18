# 本地生活智能推荐与服务编排系统改造说明

这份文档用于复习代码和面试表达。对外统一描述为“本地生活业务场景”。

## 阶段 0：现有功能稳定性

### Feed 推送修复

`BlogServiceImpl.saveBlog()` 中，发布笔记后会查出作者的粉丝，并把笔记 id 写入每个粉丝的 `feed:{userId}` ZSet。

关键点：

- `follow.getUserId()` 是粉丝 id。
- `follow.getFollowUserId()` 是被关注的人，也就是作者 id。
- 原来如果用 `follow.getId()`，拿到的是关注关系表主键，不是粉丝 id，Feed 会推错收件箱。

### Feed 滚动分页修复

`queryBlogOfFollow()` 需要返回 `Result.ok(ScrollResult)`，不能返回 `null`。

`ScrollResult` 里的字段：

- `list`：本页博客。
- `minTime`：本页最小时间戳，下一次作为 `lastId`。
- `offset`：本页中等于 `minTime` 的数量，用来处理重复 score。

## 阶段 1：缓存治理

商户详情入口是 `ShopServiceImpl.queryById()`。

当前启用的是互斥锁方案：

```java
cacheClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES)
```

三种方案的区别：

- 缓存空值：数据库不存在时写入空字符串，解决缓存穿透。
- 互斥锁：缓存失效时只有一个线程查数据库，解决缓存击穿。
- 逻辑过期：返回旧数据并异步重建缓存，适合热点数据。

## 阶段 2：Kafka 异步秒杀

秒杀入口是 `VoucherOrderServiceImpl.seckillVoucher()`。

流程：

1. Java 生成订单 id。
2. 执行 Redis Lua 脚本。
3. Lua 原子判断库存和一人一单，并预扣 Redis 库存。
4. Lua 成功后，Java 发送 `VoucherOrderMessage` 到 Kafka topic `voucher-order`。
5. `@KafkaListener` 异步消费消息。
6. 消费者用 Redisson 用户锁防重复，并调用事务方法落库。

为什么 Lua 仍然需要：

- Kafka 只负责异步削峰，不负责原子校验。
- 库存和一人一单必须在 Redis 服务端一次完成，否则高并发下会超卖或重复下单。

为什么数据库还要兜底：

- Redis 是前置校验。
- MySQL 落库时仍然再次检查一人一单和库存，防止消息重复消费或异常重试。

## 阶段 3：Elasticsearch 商户搜索

新增接口：

- `POST /shop/es/sync`：把 MySQL 商户同步到 ES。
- `GET /shop/search?keyword=火锅&minScore=40`：从 ES 搜索商户。

面试表达：

- MySQL 适合事务和权威数据存储。
- Elasticsearch 适合关键词检索和多条件搜索。
- `text` 字段用于分词搜索，`keyword` 字段用于精确匹配。

## 阶段 4：Agent 服务编排

Java 提供工具接口：

- `GET /agent/tools/shops/search`
- `GET /agent/tools/shops/detail`
- `GET /agent/tools/vouchers`
- `GET /agent/tools/blogs/hot`

Python FastAPI 服务在 `agent-service/main.py`。

启动方式：

```bash
cd agent-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

测试：

```bash
curl -X POST http://127.0.0.1:8000/agent/recommend ^
  -H "Content-Type: application/json" ^
  -d "{\"query\":\"找评分高的火锅店\"}"
```

当前版本先用规则提取关键词，后续可以接 LangChain Tool Calling。
