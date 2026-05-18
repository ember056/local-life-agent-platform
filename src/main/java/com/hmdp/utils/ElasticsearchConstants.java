package com.hmdp.utils;

public class ElasticsearchConstants {

    public static final String SHOP_INDEX = "hmdp_shop";

    public static final String SHOP_INDEX_MAPPING = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\": {\"type\": \"long\"},\n" +
            "      \"name\": {\"type\": \"text\", \"analyzer\": \"standard\"},\n" +
            "      \"typeId\": {\"type\": \"long\"},\n" +
            "      \"images\": {\"type\": \"keyword\", \"index\": false},\n" +
            "      \"area\": {\"type\": \"keyword\"},\n" +
            "      \"address\": {\"type\": \"text\", \"analyzer\": \"standard\"},\n" +
            "      \"x\": {\"type\": \"double\"},\n" +
            "      \"y\": {\"type\": \"double\"},\n" +
            "      \"avgPrice\": {\"type\": \"long\"},\n" +
            "      \"sold\": {\"type\": \"integer\"},\n" +
            "      \"comments\": {\"type\": \"integer\"},\n" +
            "      \"score\": {\"type\": \"integer\"},\n" +
            "      \"openHours\": {\"type\": \"keyword\", \"index\": false}\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private ElasticsearchConstants() {
    }
}
