# Code Walkthrough

## 1. CacheClient

Path: `src/main/java/com/hmdp/utils/CacheClient.java`

`CacheClient` 把 Redis 缓存逻辑从具体业务里抽出来，避免每个 Service 都重复写缓存代码。

重点方法：

- `set`：普通缓存写入。
- `setWithLogicalExpire`：写入带逻辑过期时间的数据。
- `queryWithPassThrough`：缓存穿透方案。
- `queryWithMutex`：互斥锁重建缓存。
- `queryWithLogicalExpire`：逻辑过期异步重建缓存。

面试讲法：

> 我没有把缓存逻辑硬编码在 ShopService 里，而是封装成泛型 CacheClient。业务方只需要传 key 前缀、id、返回类型和数据库 fallback 方法，例如 `this::getById`。

## 2. ShopServiceImpl

Path: `src/main/java/com/hmdp/service/impl/ShopServiceImpl.java`

核心职责：

- 商户详情查询。
- 商户缓存更新。
- Elasticsearch 商户搜索。
- MySQL 到 ES 的索引同步。

重要代码线：

```java
cacheClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES)
```

这行体现了：

- `CACHE_SHOP_KEY + id` 作为 Redis key。
- `Shop.class` 告诉 JSON 反序列化目标类型。
- `this::getById` 是数据库查询兜底函数。

## 3. VoucherOrderServiceImpl

Path: `src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java`

秒杀入口：

```java
public Result seckillVoucher(Long voucherId)
```

流程：

1. 获取当前用户 id。
2. 使用 RedisIdWorker 生成全局订单 id。
3. 执行 Lua 脚本。
4. 成功后发送 Kafka 消息。
5. 立即返回订单 id。

Kafka 消费：

```java
@KafkaListener(topics = KafkaConstants.VOUCHER_ORDER_TOPIC, groupId = "hmdp-voucher-order")
public void listenVoucherOrder(String message)
```

消费者把 JSON 消息转成 `VoucherOrder`，再调用 `handleVoucherOrder`。

为什么消费者里还要加锁：

- Kafka 可能重试或重复投递。
- 同一用户的多个请求可能同时进入消费者。
- 用户维度的 Redisson 锁可以保护订单创建过程。

## 4. sekill.lua

Path: `src/main/resources/sekill.lua`

Lua 脚本负责在 Redis 服务端一次性完成：

- 判断库存。
- 判断用户是否已下单。
- 扣减 Redis 库存。
- 记录用户已下单。

为什么不用 Java 分多步做：

> 多个 Redis 命令从 Java 发出时，中间可能被其他线程插入。Lua 在 Redis 服务端一次执行完，可以保证这段逻辑的原子性。

## 5. BlogServiceImpl

Path: `src/main/java/com/hmdp/service/impl/BlogServiceImpl.java`

重点能力：

- 点赞用 Redis ZSet 存储，score 是点赞时间。
- 点赞 Top5 用 ZSet range 查询。
- Feed 流用 ZSet 存每个用户的收件箱。

Feed 推送：

```java
String key = FEED_KEY + userId;
stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
```

Feed 查询：

```java
reverseRangeByScoreWithScores(key, 0, max, offset, 3)
```

`max` 和 `offset` 用来做滚动分页，并处理相同时间戳 score 的重复问题。

## 6. AgentToolController

Path: `src/main/java/com/hmdp/controller/AgentToolController.java`

这个 Controller 不是给普通前端页面用的，而是给 Agent 服务调用的。

它把后端能力包装成工具：

- `/agent/tools/shops/search`
- `/agent/tools/shops/detail`
- `/agent/tools/vouchers`
- `/agent/tools/blogs/hot`

面试讲法：

> Agent 不直接连数据库，而是调用后端提供的稳定工具接口。这样业务权限、缓存、搜索、优惠券查询都复用 Java 后端能力，Agent 只负责编排。
