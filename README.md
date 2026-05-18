# Local Life Intelligent Recommendation and Service Orchestration

一个围绕本地生活业务场景构建的后端项目，覆盖商户浏览、优惠券秒杀、Redis 缓存治理、关注 Feed 流、Elasticsearch 商户搜索，以及面向 Agent 的工具接口与 FastAPI 推荐服务骨架。

> 项目定位：面向 Java 后端与 Agent 应用开发岗位，重点展示高并发秒杀、缓存治理、搜索能力和服务编排思路。

## Highlights

- **Redis 缓存治理**：商户详情支持缓存空值、互斥锁、逻辑过期三种方案，缓解缓存穿透和缓存击穿。
- **异步秒杀链路**：Redis Lua 原子校验库存与一人一单，Kafka 异步削峰，Redisson 用户锁与数据库事务兜底。
- **关注 Feed 流**：基于 Redis ZSet 实现推模式 Feed，支持滚动分页和重复 score 处理。
- **商户搜索**：Elasticsearch 承载商户关键词、多条件检索，区别于 MySQL 模糊查询。
- **Agent 工具接口**：Java 后端提供商户搜索、商户详情、优惠券、热门笔记等工具接口，FastAPI 服务负责编排推荐流程。

## Architecture

```text
Frontend/Nginx
    |
Spring Boot API
    |-- MySQL: users, shops, vouchers, orders, blogs
    |-- Redis: login token, cache, Lua seckill stock, likes, feed inbox
    |-- Redisson: per-user order lock
    |-- Kafka: async voucher order events
    |-- Elasticsearch: shop search index
    |
FastAPI Agent Service
    |-- calls /agent/tools/*
    |-- returns structured recommendation result
```

更详细设计见 [docs/architecture.md](docs/architecture.md)。

## Tech Stack

- Java 8, Spring Boot 2.3, MyBatis-Plus
- Redis, Redisson, Lua
- Kafka
- Elasticsearch 7.x
- MySQL 5.7
- FastAPI, Pydantic, httpx

## Quick Start

### 1. Start infrastructure

```bash
docker compose up -d mysql redis zookeeper kafka elasticsearch
```

### 2. Configure environment

Copy `.env.example` and adjust values if needed.

```bash
cp .env.example .env
```

For local IDE runs, make sure these defaults are available:

```text
MySQL: 127.0.0.1:3306 / hmdp / root / root
Redis: 127.0.0.1:6379
Kafka: 127.0.0.1:9092
Elasticsearch: 127.0.0.1:9200
```

### 3. Start Spring Boot

Run `com.hmdp.HmDianPingApplication` in IDEA, or:

```bash
mvn spring-boot:run
```

### 4. Sync shop data to Elasticsearch

```bash
curl -X POST http://127.0.0.1:8081/shop/es/sync
```

### 5. Start Agent service

```bash
cd agent-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

Test:

```bash
curl -X POST http://127.0.0.1:8000/agent/recommend ^
  -H "Content-Type: application/json" ^
  -d "{\"query\":\"找评分高的火锅店\"}"
```

## Key APIs

| Module | Method | Path | Description |
| --- | --- | --- | --- |
| Shop | GET | `/shop/{id}` | Query shop detail with Redis cache |
| Shop Search | POST | `/shop/es/sync` | Sync MySQL shops to ES |
| Shop Search | GET | `/shop/search` | Search shops from ES |
| Voucher | POST | `/voucher-order/seckill/{id}` | Seckill voucher, async order creation |
| Blog | GET | `/blog/hot` | Hot blogs |
| Feed | GET | `/blog/of/follow` | Follow feed scroll query |
| Agent Tools | GET | `/agent/tools/shops/search` | Tool endpoint for Agent |

More details: [docs/api.md](docs/api.md).

## Interview Guide

如果面试官问“这个项目最核心的设计是什么”，可以按这条线讲：

1. 本地生活业务包含商户、优惠券、用户互动和内容流。
2. 商户详情高频访问，因此引入 Redis 缓存治理。
3. 秒杀是高并发写场景，因此用 Lua 做原子校验，用 Kafka 异步削峰，用数据库事务兜底。
4. 商户搜索从 MySQL like 升级到 Elasticsearch。
5. 为 Agent 推荐服务提供工具接口，让自然语言推荐可以复用后端能力。

详见 [docs/interview-guide.md](docs/interview-guide.md)。

## Repository Notes

- Do not commit local passwords. Use environment variables or `.env`.
- `application.yaml` uses safe defaults and environment placeholders.
- Generated files, IDE files, and build output are ignored by `.gitignore`.
