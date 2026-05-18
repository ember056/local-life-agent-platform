from typing import Any

import httpx
from fastapi import FastAPI
from pydantic import BaseModel, Field


JAVA_API_BASE = "http://127.0.0.1:8081"

app = FastAPI(title="Local Life Recommendation Agent")


class RecommendRequest(BaseModel):
    query: str = Field(..., description="Natural language user request")
    type_id: int | None = None
    min_score: int | None = None


class RecommendResponse(BaseModel):
    intent: str
    shops: list[dict[str, Any]]
    reason: str
    tool_trace: list[str]


def extract_keyword(query: str) -> str:
    for word in ["火锅", "茶餐厅", "KTV", "酒吧", "美容", "按摩", "健身", "美食"]:
        if word in query:
            return word
    return query


@app.post("/agent/recommend", response_model=RecommendResponse)
async def recommend(request: RecommendRequest) -> RecommendResponse:
    keyword = extract_keyword(request.query)
    params = {
        "keyword": keyword,
        "current": 1,
    }
    if request.type_id is not None:
        params["typeId"] = request.type_id
    if request.min_score is not None:
        params["minScore"] = request.min_score

    async with httpx.AsyncClient(timeout=5.0) as client:
        shop_resp = await client.get(f"{JAVA_API_BASE}/agent/tools/shops/search", params=params)
        shop_resp.raise_for_status()
        payload = shop_resp.json()

    shops = payload.get("data") or []
    top_shops = shops[:3]
    return RecommendResponse(
        intent="shop_recommendation",
        shops=top_shops,
        reason=f"根据你的需求提取关键词 `{keyword}`，调用商户搜索工具后返回前 {len(top_shops)} 个候选。",
        tool_trace=[
            "extract_keyword",
            "GET /agent/tools/shops/search",
        ],
    )
