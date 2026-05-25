import os
from typing import Any

import httpx
from fastapi import FastAPI
from pydantic import BaseModel, Field


JAVA_API_BASE = os.getenv("JAVA_API_BASE", "http://127.0.0.1:8081")
MOCK_ENABLED = os.getenv("AGENT_MOCK_ENABLED", "true").lower() != "false"

app = FastAPI(title="Local Life Recommendation Agent")


class RecommendRequest(BaseModel):
    query: str = Field(..., description="Natural language user request")
    type_id: int | None = None
    min_score: int | None = None


class ShopRecommendation(BaseModel):
    shop: dict[str, Any]
    vouchers: list[dict[str, Any]]
    reasons: list[str]


class RecommendResponse(BaseModel):
    intent: str
    keyword: str
    shops: list[dict[str, Any]]
    recommendations: list[ShopRecommendation]
    hot_blogs: list[dict[str, Any]]
    reason: str
    tool_trace: list[str]
    mock: bool


MOCK_SHOPS = [
    {
        "id": 1,
        "name": "老城铜锅涮肉",
        "typeId": 1,
        "area": "解放碑",
        "address": "解放碑步行街 88 号",
        "avgPrice": 92,
        "score": 93,
        "sold": 2680,
        "comments": 740,
        "openHours": "10:00-22:30",
    },
    {
        "id": 2,
        "name": "山城九宫格火锅",
        "typeId": 1,
        "area": "观音桥",
        "address": "观音桥商圈 A 座 3 楼",
        "avgPrice": 78,
        "score": 88,
        "sold": 3510,
        "comments": 1120,
        "openHours": "11:00-02:00",
    },
    {
        "id": 3,
        "name": "春日茶餐厅",
        "typeId": 2,
        "area": "大学城",
        "address": "大学城南路 18 号",
        "avgPrice": 46,
        "score": 86,
        "sold": 1290,
        "comments": 320,
        "openHours": "08:30-21:30",
    },
    {
        "id": 4,
        "name": "夜色 KTV",
        "typeId": 3,
        "area": "五一广场",
        "address": "五一广场 12 号",
        "avgPrice": 128,
        "score": 82,
        "sold": 760,
        "comments": 210,
        "openHours": "13:00-03:00",
    },
]

MOCK_VOUCHERS = {
    1: [
        {"id": 101, "title": "满 100 减 25", "payValue": 7500, "actualValue": 10000},
        {"id": 102, "title": "双人套餐立减 40", "payValue": 15800, "actualValue": 19800},
    ],
    2: [
        {"id": 201, "title": "工作日 8.8 折券", "payValue": 8800, "actualValue": 10000},
    ],
    3: [
        {"id": 301, "title": "下午茶套餐券", "payValue": 3900, "actualValue": 5200},
    ],
}

MOCK_HOT_BLOGS = [
    {"id": 9001, "title": "本周火锅热门榜单", "liked": 421, "shopId": 1},
    {"id": 9002, "title": "学生党高性价比聚餐清单", "liked": 318, "shopId": 3},
    {"id": 9003, "title": "夜宵场景怎么选店", "liked": 276, "shopId": 2},
]

KEYWORD_CANDIDATES = ["火锅", "茶餐厅", "KTV", "酒吧", "美容", "按摩", "健身", "美食", "烧烤", "咖啡"]
TYPE_HINTS = {
    "火锅": 1,
    "美食": 1,
    "茶餐厅": 2,
    "KTV": 3,
}


def extract_keyword(query: str) -> str:
    for word in KEYWORD_CANDIDATES:
        if word.lower() in query.lower():
            return word
    return query.strip()[:20] or "美食"


def infer_type_id(keyword: str, explicit_type_id: int | None) -> int | None:
    if explicit_type_id is not None:
        return explicit_type_id
    return TYPE_HINTS.get(keyword)


def infer_min_score(query: str, explicit_min_score: int | None) -> int | None:
    if explicit_min_score is not None:
        return explicit_min_score
    if "评分高" in query or "高分" in query or "好评" in query:
        return 85
    return None


def result_data(payload: dict[str, Any]) -> Any:
    if isinstance(payload, dict):
        return payload.get("data", payload)
    return payload


def mock_search_shops(keyword: str, type_id: int | None, min_score: int | None) -> list[dict[str, Any]]:
    shops = MOCK_SHOPS
    if type_id is not None:
        shops = [shop for shop in shops if shop.get("typeId") == type_id]
    if min_score is not None:
        shops = [shop for shop in shops if shop.get("score", 0) >= min_score]
    if keyword:
        matched = [
            shop for shop in shops
            if keyword.lower() in shop.get("name", "").lower()
            or keyword.lower() in shop.get("address", "").lower()
            or keyword.lower() in shop.get("area", "").lower()
        ]
        if matched:
            shops = matched
    return sorted(shops, key=lambda item: (item.get("score", 0), item.get("sold", 0)), reverse=True)


def mock_shop_detail(shop_id: int) -> dict[str, Any]:
    for shop in MOCK_SHOPS:
        if shop["id"] == shop_id:
            detail = dict(shop)
            detail["recommendTags"] = ["评分稳定", "适合聚餐", "优惠可用"]
            return detail
    return {"id": shop_id, "name": "模拟商户", "score": 80, "recommendTags": ["兜底推荐"]}


def mock_vouchers(shop_id: int) -> list[dict[str, Any]]:
    return MOCK_VOUCHERS.get(shop_id, [])


def mock_hot_blogs() -> list[dict[str, Any]]:
    return MOCK_HOT_BLOGS


async def get_tool_data(
    client: httpx.AsyncClient,
    path: str,
    params: dict[str, Any] | None,
    fallback: Any,
    trace: list[str],
) -> Any:
    trace.append(f"CALL {path} params={params or {}}")
    if MOCK_ENABLED:
        trace.append(f"MOCK {path}")
        return fallback

    try:
        response = await client.get(f"{JAVA_API_BASE}{path}", params=params)
        response.raise_for_status()
        trace.append(f"OK {path}")
        return result_data(response.json())
    except Exception as exc:
        trace.append(f"FALLBACK {path}: {exc.__class__.__name__}")
        return fallback


def build_reasons(shop: dict[str, Any], vouchers: list[dict[str, Any]], keyword: str) -> list[str]:
    reasons = []
    score = shop.get("score")
    sold = shop.get("sold")
    if score is not None:
        reasons.append(f"评分 {score}，符合 `{keyword}` 场景下的质量筛选。")
    if sold is not None:
        reasons.append(f"近期销量约 {sold}，说明用户选择较多。")
    if vouchers:
        reasons.append(f"当前有 {len(vouchers)} 张可用优惠券，适合优先推荐。")
    if shop.get("area"):
        reasons.append(f"位于 {shop['area']}，方便按商圈继续筛选。")
    return reasons or ["命中用户需求关键词，作为候选商户返回。"]


@app.get("/health")
async def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "mock": MOCK_ENABLED,
        "java_api_base": JAVA_API_BASE,
    }


@app.post("/agent/recommend", response_model=RecommendResponse)
async def recommend(request: RecommendRequest) -> RecommendResponse:
    trace: list[str] = []
    keyword = extract_keyword(request.query)
    type_id = infer_type_id(keyword, request.type_id)
    min_score = infer_min_score(request.query, request.min_score)
    trace.append(f"INTENT shop_recommendation keyword={keyword} typeId={type_id} minScore={min_score}")

    search_params = {
        "keyword": keyword,
        "current": 1,
    }
    if type_id is not None:
        search_params["typeId"] = type_id
    if min_score is not None:
        search_params["minScore"] = min_score

    mock_shops = mock_search_shops(keyword, type_id, min_score)

    async with httpx.AsyncClient(timeout=5.0) as client:
        shops = await get_tool_data(
            client,
            "/agent/tools/shops/search",
            search_params,
            mock_shops,
            trace,
        )
        shops = shops or mock_shops
        top_shops = list(shops)[:3]

        recommendations: list[ShopRecommendation] = []
        for shop in top_shops:
            shop_id = int(shop.get("id"))
            detail = await get_tool_data(
                client,
                "/agent/tools/shops/detail",
                {"id": shop_id},
                mock_shop_detail(shop_id),
                trace,
            )
            detail = detail or shop
            vouchers = await get_tool_data(
                client,
                "/agent/tools/vouchers",
                {"shopId": shop_id},
                mock_vouchers(shop_id),
                trace,
            )
            vouchers = vouchers or []
            recommendations.append(
                ShopRecommendation(
                    shop=detail,
                    vouchers=vouchers,
                    reasons=build_reasons(detail, vouchers, keyword),
                )
            )

        hot_blogs = await get_tool_data(
            client,
            "/agent/tools/blogs/hot",
            {"current": 1},
            mock_hot_blogs(),
            trace,
        )
        hot_blogs = hot_blogs or []

    reason = (
        f"根据用户需求 `{request.query}`，Mock Agent 提取关键词 `{keyword}`，"
        "按商户搜索、详情补全、优惠券查询、热门笔记查询的顺序完成工具编排，"
        f"最终返回 {len(recommendations)} 个结构化推荐结果。"
    )

    return RecommendResponse(
        intent="shop_recommendation",
        keyword=keyword,
        shops=[item.shop for item in recommendations],
        recommendations=recommendations,
        hot_blogs=list(hot_blogs)[:3],
        reason=reason,
        tool_trace=trace,
        mock=MOCK_ENABLED,
    )
