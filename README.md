# Local Life Agent Platform

一个面向本地生活业务场景的后端项目，覆盖商户浏览、优惠券秒杀、Redis 缓存治理、关注 Feed 流、Elasticsearch 商户搜索，以及面向 Agent 的工具接口和 FastAPI 推荐服务骨架。

项目定位：面向 Java 后端与 Agent 应用开发岗位，重点展示高并发秒杀、缓存治理、搜索能力和服务编排思路。

## 项目亮点

- Redis 缓存治理：商户详情支持缓存空值、互斥锁、逻辑过期三种方案，缓解缓存穿透与缓存击穿。
- 异步秒杀链路：Redis Lua 原子校验库存与一人一单，Kafka 异步削峰，Redisson 用户锁与数据库事务兜底。
- 关注 Feed 流：基于 Redis ZSet 实现推模式 Feed，支持滚动分页和重复 score 处理。
- 商户搜索：Elasticsearch 承载商户关键词检索，区别于 MySQL 模糊查询。
- Agent 工具接口：Java 后端提供商户搜索、商户详情、优惠券、热门笔记等工具接口，FastAPI 服务负责编排推荐流程。

## 架构概览

```text
Frontend / Nginx
    |
Spring Boot API
    |-- MySQL: 用户、商户、优惠券、订单、笔记
    |-- Redis: 登录 token、缓存、Lua 秒杀库存、点赞、Feed 收件箱
    |-- Redisson: 用户维度分布式锁
    |-- Kafka: 异步订单消息
    |-- Elasticsearch: 商户搜索索引
    |
FastAPI Agent Service
    |-- 调用 /agent/tools/*
    |-- 返回结构化推荐结果
```

详细设计见 [docs/架构设计.md](docs/架构设计.md)。

## 技术栈

- Java 8, Spring Boot 2.3, MyBatis-Plus
- Redis, Redisson, Lua
- Kafka
- Elasticsearch
- MySQL
- FastAPI

## 目录结构

```text
.
|-- src/                    Java 后端代码
|-- agent-service/           FastAPI Agent 服务
|-- docs/                    中文项目文档
|-- docker-compose.yml       本地依赖编排
|-- .env.example             环境变量示例
`-- README.md
```

## 快速启动

1. 准备 MySQL、Redis、Kafka、Elasticsearch。
2. 复制 `.env.example`，按本机环境配置数据库和中间件连接信息。
3. 导入 `src/main/resources/db/hmdp.sql`。
4. 启动 Java 后端：

```bash
mvn spring-boot:run
```

5. 启动 Agent 服务：

```bash
cd agent-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

## 核心接口

- `GET /shop/{id}`：查询商户详情，走缓存治理链路。
- `GET /shop/search`：商户搜索，走 Elasticsearch。
- `POST /voucher-order/seckill/{id}`：优惠券秒杀，走 Lua 与 Kafka 异步链路。
- `GET /blog/hot`：热门笔记。
- `GET /follow/common/{id}`：共同关注。
- `GET /agent/tools/shops/search`：Agent 商户搜索工具接口。

更多接口见 [docs/接口文档.md](docs/接口文档.md)。

## 文档导航

- [架构设计](docs/架构设计.md)
- [代码详解](docs/代码详解.md)
- [接口文档](docs/接口文档.md)
- [部署运行](docs/部署运行.md)
- [面试讲解](docs/面试讲解.md)
- [项目改造记录](docs/项目改造记录.md)
- [GitHub 上传清单](docs/GitHub上传清单.md)

## 面试表达

这个项目可以按照“业务入口 -> 缓存治理 -> 秒杀削峰 -> 搜索能力 -> Agent 服务编排”的顺序讲解。它不是简单 CRUD，而是围绕本地生活场景，把高并发、缓存、消息队列、搜索和智能推荐串成了一条完整链路。
