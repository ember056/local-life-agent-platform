# Interview Guide

## 1. 30-Second Pitch

这是一个本地生活业务系统，包含商户浏览、优惠券秒杀、探店笔记、关注 Feed 和商户搜索。我在传统 Spring Boot 业务的基础上，重点做了 Redis 缓存治理、Lua + Kafka 的异步秒杀、Elasticsearch 搜索，以及给 Agent 服务调用的工具接口。

## 2. Recommended Explanation Order

### Step 1: Business Background

先讲业务：

> 用户可以浏览商户、领取和秒杀优惠券、发布探店笔记、关注其他用户并查看关注流。后面我又扩展了商户搜索和 Agent 推荐服务。

### Step 2: Cache

重点讲商户详情：

> 商户详情是高频读接口，直接查数据库会有压力。我封装了 CacheClient，支持缓存空值、互斥锁和逻辑过期三种模式。

可以补一句：

> 缓存空值解决穿透，互斥锁解决击穿，逻辑过期适合热点数据。

### Step 3: Seckill

秒杀按链路讲：

```text
请求进入
  -> Redis Lua 校验库存和一人一单
  -> 预扣 Redis 库存
  -> Kafka 投递订单消息
  -> 消费者异步落库
  -> MySQL 事务兜底
```

关键表达：

> Lua 解决原子校验问题，Kafka 解决削峰问题，数据库事务解决最终落库一致性问题。

### Step 4: Feed

讲推模式：

> 用户发布笔记时，我查询粉丝列表，把 blogId 写入每个粉丝的 Redis ZSet 收件箱。score 是时间戳，查询时用倒序范围查询实现滚动分页。

如果问重复 score：

> 返回 minTime 和 offset。下一页用 minTime 作为 max，用 offset 跳过已经读取过的同分数据。

### Step 5: Elasticsearch

讲职责拆分：

> MySQL 存权威数据，Elasticsearch 存搜索文档。商户搜索不再依赖 MySQL like，而是通过 ES 做关键词和多条件检索。

### Step 6: Agent

讲服务编排：

> 我没有让 Agent 直接查数据库，而是给它提供工具接口，比如商户搜索、商户详情、优惠券查询。Agent 服务负责理解自然语言并编排这些工具。

## 3. Questions and Answers

### Q: 为什么秒杀里用了 Redis Lua 后还需要数据库检查？

Redis 是前置校验，能挡住大部分高并发请求。但消息可能重复消费，或者系统异常重试，所以 MySQL 落库前仍然要检查一人一单和库存，做最终兜底。

### Q: Kafka 和 Redis Stream 有什么区别？

Redis Stream 更轻量，适合项目早期或 Redis 内部闭环。Kafka 更适合独立消息中间件场景，有更成熟的消费组、堆积能力和生态，简历项目里更能体现异步削峰和服务解耦。

### Q: 为什么 Agent 服务单独用 FastAPI？

Agent 更偏 AI 编排和工具调用，Python 生态更成熟；Java 后端继续负责稳定业务接口。两者通过 HTTP 工具接口解耦。

### Q: 如果 Kafka 消费失败怎么办？

当前版本依赖 Kafka 消费者重试和日志定位。生产环境可以进一步加死信队列、重试次数、消息状态表和补偿任务。

### Q: ES 数据和 MySQL 不一致怎么办？

当前版本提供手动同步接口。生产环境可以通过 Canal、消息队列或业务写入时双写/异步同步来保证 ES 最终一致。

## 4. What to Demo

推荐演示顺序：

1. 首页商户和博客能访问。
2. 访问商户详情，说明 Redis 缓存。
3. 调用秒杀接口，说明立即返回订单 id，订单异步创建。
4. 调用 `/shop/es/sync`，再调用 `/shop/search`。
5. 启动 FastAPI，调用 `/agent/recommend`。

## 5. Resume Bullets

可以写：

- 针对商户详情高频访问，封装 Redis 缓存组件，支持缓存空值、互斥锁和逻辑过期策略，缓解缓存穿透与缓存击穿。
- 针对优惠券秒杀库存超卖和重复下单问题，使用 Redis Lua 原子校验库存与用户下单状态，并通过 Kafka 异步投递订单事件削峰。
- 针对商户模糊搜索效率低的问题，引入 Elasticsearch 建立商户索引，支持关键词、分类、评分等多条件检索。
- 面向自然语言找店场景，设计 Agent 工具接口，使用 FastAPI 编排商户搜索、优惠券查询和商户详情工具，返回结构化推荐结果。
