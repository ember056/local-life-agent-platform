# Architecture Design

## 1. Project Goal

本项目面向本地生活业务场景，提供商户浏览、优惠券秒杀、探店笔记、关注 Feed、商户搜索与 Agent 推荐服务编排能力。

核心目标不是堆功能，而是展示几个后端关键能力：

- 高频读场景的缓存治理。
- 高并发写场景的异步削峰与一致性兜底。
- 搜索场景从数据库查询到搜索引擎的拆分。
- 面向 Agent 的工具接口设计。

## 2. System View

```text
                  +-----------------------+
                  |  Web / Mobile Client  |
                  +-----------+-----------+
                              |
                              v
                  +-----------------------+
                  |    Spring Boot API    |
                  +-----------+-----------+
                              |
        +---------------------+----------------------+
        |                     |                      |
        v                     v                      v
   +---------+           +---------+            +-----------+
   |  MySQL  |           |  Redis  |            |   Kafka   |
   +---------+           +---------+            +-----------+
        |                     |                      |
        |                     v                      v
        |              cache / locks / feed     async orders
        |
        v
 +----------------+
 | Elasticsearch  |
 +----------------+
        ^
        |
 +---------------------+
 | FastAPI Agent       |
 | calls /agent/tools  |
 +---------------------+
```

## 3. Core Modules

### User and Login

- 手机号验证码登录。
- Redis Hash 存储登录用户 DTO。
- `RefreshTokenInterceptor` 负责解析 token 并刷新 TTL。
- `LoginInterceptor` 负责保护需要登录的接口。

### Shop Cache

商户详情是典型高频读接口。

当前抽象在 `CacheClient`：

- `queryWithPassThrough`：解决缓存穿透。
- `queryWithMutex`：解决缓存击穿。
- `queryWithLogicalExpire`：适合热点商户的逻辑过期。

### Voucher Seckill

秒杀链路拆成两个阶段：

```text
Request thread:
  validate login
  generate orderId
  execute Redis Lua
  send Kafka message
  return orderId

Kafka consumer:
  consume VoucherOrderMessage
  acquire Redisson user lock
  check one-user-one-order in MySQL
  deduct DB stock
  insert order
```

这样做的原因：

- Redis Lua 保证库存和一人一单的原子校验。
- Kafka 削峰，避免请求线程直接写数据库。
- MySQL 事务做最终一致性兜底。

### Feed

关注 Feed 使用推模式：

```text
author publishes blog
  -> query followers
  -> write blogId into feed:{followerId} ZSet
```

ZSet 的 value 是 `blogId`，score 是时间戳。查询时用 `reverseRangeByScoreWithScores` 实现按时间倒序滚动分页。

### Search

商户搜索通过 Elasticsearch 实现。

- MySQL 保留权威数据。
- ES 存储搜索文档 `ShopDoc`。
- `/shop/es/sync` 将 MySQL 商户同步到 ES。
- `/shop/search` 按关键词、分类、最低评分检索商户。

### Agent Tools

`AgentToolController` 提供稳定工具接口：

- 商户搜索
- 商户详情
- 优惠券列表
- 热门笔记

FastAPI Agent 服务不直接访问数据库，只调用这些工具接口。这样 Java 后端负责业务能力，Agent 服务负责编排和自然语言交互。

## 4. Consistency Strategy

秒杀场景采用多层保护：

- Redis Lua：前置原子校验和预扣库存。
- Kafka：异步投递订单事件。
- Redisson：消费者侧按用户加锁，避免同一用户并发创建订单。
- MySQL：事务内再次检查订单和库存。

这不是强一致的分布式事务，而是适合秒杀场景的最终一致设计。
