package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopDoc;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.ElasticsearchConstants.SHOP_INDEX;
import static com.hmdp.utils.ElasticsearchConstants.SHOP_INDEX_MAPPING;
import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final StringRedisTemplate stringRedisTemplate;

    public ShopServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Resource
    public CacheClient cacheClient;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Override
    public Result queryById(Long id) {

        //缓存穿透
        //Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        //互斥锁解决缓存击穿
        Shop shop = cacheClient.queryWithMutex(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        //逻辑过期解决缓存击穿
        //Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if (shop==null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

//    public Shop queryWithPassThrough(Long id){
//        String key= CACHE_SHOP_KEY + id;
//        //从redis查询商铺缓存
//        String shopjson = stringRedisTemplate.opsForValue().get(key);
//        //判断是否存在
//        if (StrUtil.isNotBlank(shopjson)) {
//            //存在直接返回信息
//            return JSONUtil.toBean(shopjson, Shop.class);
//        }
//        if (shopjson!=null) {
//            return null;
//        }
//        //不存在根据id查询数据库
//        Shop shop = getById(id);
//        //不存在返回错误
//        if (shop==null) {
//            //空值写进redis
//            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            //返回错误信息
//            return null;
//        }
//        //存在写入redis
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        return shop;
//    }
//    public Shop queryWithMutex(Long id){
//        String key= CACHE_SHOP_KEY + id;
//        //从redis查询商铺缓存
//        String shopjson = stringRedisTemplate.opsForValue().get(key);
//        //判断是否存在
//        if (StrUtil.isNotBlank(shopjson)) {
//            //存在直接返回信息
//            return JSONUtil.toBean(shopjson, Shop.class);
//        }
//        if (shopjson!=null) {
//            return null;
//        }
//        //实现缓存重建*
//        //获取互斥锁
//        String lockKey="lock:shop:"+id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            //判断是否获取成功
//            if (!isLock) {
//                Thread.sleep(50);
//                //失败就休眠并重试
//                queryWithMutex(id);
//            }
//            //成功就根据id查询数据库
//            shop = getById(id);
//            //不存在返回错误
//            if (shop==null) {
//                //空值写进redis
//                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//                //返回错误信息
//                return null;
//            }
//            //存在写入redis
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            //释放互斥锁
//            unLock(lockKey);
//        }
//        //返回
//        return shop;
//    }
//    private boolean tryLock(String key){
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//    private void unLock(String key){
//        stringRedisTemplate.delete(key);
//    }

//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
//
//    public Shop queryWithLogicalExpire(Long id){
//        String key= CACHE_SHOP_KEY + id;
//        //从redis查询商铺缓存
//        String shopjson = stringRedisTemplate.opsForValue().get(key);
//        //判断是否存在
//        if (StrUtil.isBlank(shopjson)) {
//            //存在直接返回信息
//            return null;
//        }
//        //命中，需要先吧json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopjson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        //判断是否过期
//        if (expireTime.isAfter((LocalDateTime.now()))) {
//            //未过期，直接返回店铺信息
//            return shop;
//        }
//        //已过期，需要缓存重建
//        //实现缓存重建*
//        //获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        //判断是否获取锁成功
//        if (isLock) {
//            //成功就开启独立线程
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    this.saveShop2Redis(id,20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    unLock(lockKey);
//                }
//            });
//        }
//        //不成功返回旧信息
//        return shop;
//    }

    public void saveShop2Redis(Long id,Long expireSeconds) throws InterruptedException{
        //查询店铺数据
        Shop shop = getById(id);
        //封装逻辑过期时间
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id==null) {
            return Result.fail("店铺id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    @Override
    public Result search(String keyword, Long typeId, Integer minScore, Integer current) {
        try {
            int pageNo = current == null || current < 1 ? 1 : current;
            int from = (pageNo - 1) * DEFAULT_PAGE_SIZE;
            SearchRequest request = new SearchRequest(SHOP_INDEX);
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (StrUtil.isNotBlank(keyword)) {
                boolQuery.must(QueryBuilders.multiMatchQuery(keyword, "name", "address"));
            } else {
                boolQuery.must(QueryBuilders.matchAllQuery());
            }
            if (typeId != null) {
                boolQuery.filter(QueryBuilders.termQuery("typeId", typeId));
            }
            if (minScore != null) {
                boolQuery.filter(QueryBuilders.rangeQuery("score").gte(minScore));
            }
            request.source(new SearchSourceBuilder()
                    .query(boolQuery)
                    .from(from)
                    .size(DEFAULT_PAGE_SIZE));
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            List<Shop> shops = new ArrayList<>();
            for (SearchHit hit : response.getHits().getHits()) {
                ShopDoc doc = JSONUtil.toBean(hit.getSourceAsString(), ShopDoc.class);
                Shop shop = new Shop();
                shop.setId(doc.getId());
                shop.setName(doc.getName());
                shop.setTypeId(doc.getTypeId());
                shop.setImages(doc.getImages());
                shop.setArea(doc.getArea());
                shop.setAddress(doc.getAddress());
                shop.setX(doc.getX());
                shop.setY(doc.getY());
                shop.setAvgPrice(doc.getAvgPrice());
                shop.setSold(doc.getSold());
                shop.setComments(doc.getComments());
                shop.setScore(doc.getScore());
                shop.setOpenHours(doc.getOpenHours());
                shops.add(shop);
            }
            return Result.ok(shops);
        } catch (IOException e) {
            throw new RuntimeException("search shop from elasticsearch failed", e);
        }
    }

    @Override
    public Result syncToEs() {
        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest(SHOP_INDEX);
            boolean exists = restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
            if (exists) {
                restHighLevelClient.indices().delete(new DeleteIndexRequest(SHOP_INDEX), RequestOptions.DEFAULT);
            }
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(SHOP_INDEX);
            createIndexRequest.source(SHOP_INDEX_MAPPING, XContentType.JSON);
            restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);

            List<Shop> shops = list();
            BulkRequest bulkRequest = new BulkRequest();
            for (Shop shop : shops) {
                ShopDoc doc = new ShopDoc(shop);
                bulkRequest.add(new IndexRequest(SHOP_INDEX)
                        .id(shop.getId().toString())
                        .source(JSONUtil.toJsonStr(doc), XContentType.JSON));
            }
            if (!shops.isEmpty()) {
                restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            }
            return Result.ok(shops.size());
        } catch (IOException e) {
            throw new RuntimeException("sync shop to elasticsearch failed", e);
        }
    }
}
