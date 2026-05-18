# Local Life Intelligent Recommendation and Service Orchestration

涓€涓洿缁曟湰鍦扮敓娲讳笟鍔″満鏅瀯寤虹殑鍚庣椤圭洰锛岃鐩栧晢鎴锋祻瑙堛€佷紭鎯犲埜绉掓潃銆丷edis 缂撳瓨娌荤悊銆佸叧娉?Feed 娴併€丒lasticsearch 鍟嗘埛鎼滅储锛屼互鍙婇潰鍚?Agent 鐨勫伐鍏锋帴鍙ｄ笌 FastAPI 鎺ㄨ崘鏈嶅姟楠ㄦ灦銆?

> 椤圭洰瀹氫綅锛氶潰鍚?Java 鍚庣涓?Agent 搴旂敤寮€鍙戝矖浣嶏紝閲嶇偣灞曠ず楂樺苟鍙戠鏉€銆佺紦瀛樻不鐞嗐€佹悳绱㈣兘鍔涘拰鏈嶅姟缂栨帓鎬濊矾銆?

## Highlights

- **Redis 缂撳瓨娌荤悊**锛氬晢鎴疯鎯呮敮鎸佺紦瀛樼┖鍊笺€佷簰鏂ラ攣銆侀€昏緫杩囨湡涓夌鏂规锛岀紦瑙ｇ紦瀛樼┛閫忓拰缂撳瓨鍑荤┛銆?
- **寮傛绉掓潃閾捐矾**锛歊edis Lua 鍘熷瓙鏍￠獙搴撳瓨涓庝竴浜轰竴鍗曪紝Kafka 寮傛鍓婂嘲锛孯edisson 鐢ㄦ埛閿佷笌鏁版嵁搴撲簨鍔″厹搴曘€?
- **鍏虫敞 Feed 娴?*锛氬熀浜?Redis ZSet 瀹炵幇鎺ㄦā寮?Feed锛屾敮鎸佹粴鍔ㄥ垎椤靛拰閲嶅 score 澶勭悊銆?
- **鍟嗘埛鎼滅储**锛欵lasticsearch 鎵胯浇鍟嗘埛鍏抽敭璇嶃€佸鏉′欢妫€绱紝鍖哄埆浜?MySQL 妯＄硦鏌ヨ銆?
- **Agent 宸ュ叿鎺ュ彛**锛欽ava 鍚庣鎻愪緵鍟嗘埛鎼滅储銆佸晢鎴疯鎯呫€佷紭鎯犲埜銆佺儹闂ㄧ瑪璁扮瓑宸ュ叿鎺ュ彛锛孎astAPI 鏈嶅姟璐熻矗缂栨帓鎺ㄨ崘娴佺▼銆?

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

鏇磋缁嗚璁¤ [docs/架构设计.md](docs/架构设计.md)銆?

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
  -d "{\"query\":\"鎵捐瘎鍒嗛珮鐨勭伀閿呭簵\"}"
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

More details: [docs/接口文档.md](docs/接口文档.md).

## Interview Guide

濡傛灉闈㈣瘯瀹橀棶鈥滆繖涓」鐩渶鏍稿績鐨勮璁℃槸浠€涔堚€濓紝鍙互鎸夎繖鏉＄嚎璁诧細

1. 鏈湴鐢熸椿涓氬姟鍖呭惈鍟嗘埛銆佷紭鎯犲埜銆佺敤鎴蜂簰鍔ㄥ拰鍐呭娴併€?
2. 鍟嗘埛璇︽儏楂橀璁块棶锛屽洜姝ゅ紩鍏?Redis 缂撳瓨娌荤悊銆?
3. 绉掓潃鏄珮骞跺彂鍐欏満鏅紝鍥犳鐢?Lua 鍋氬師瀛愭牎楠岋紝鐢?Kafka 寮傛鍓婂嘲锛岀敤鏁版嵁搴撲簨鍔″厹搴曘€?
4. 鍟嗘埛鎼滅储浠?MySQL like 鍗囩骇鍒?Elasticsearch銆?
5. 涓?Agent 鎺ㄨ崘鏈嶅姟鎻愪緵宸ュ叿鎺ュ彛锛岃鑷劧璇█鎺ㄨ崘鍙互澶嶇敤鍚庣鑳藉姏銆?

璇﹁ [docs/面试讲解.md](docs/面试讲解.md)銆?

## Repository Notes

- Do not commit local passwords. Use environment variables or `.env`.
- `application.yaml` uses safe defaults and environment placeholders.
- Generated files, IDE files, and build output are ignored by `.gitignore`.


