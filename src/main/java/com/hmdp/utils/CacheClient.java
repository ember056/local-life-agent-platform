package com.hmdp.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }
    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R>type, Function<ID,R>dbFallback,Long time,TimeUnit unit){
        String key= keyPrefix + id;
        //从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //存在直接返回信息
            return JSONUtil.toBean(json, type);
        }
        if (json!=null) {
            return null;
        }
        //不存在根据id查询数据库
        R r = dbFallback.apply(id);
        //不存在返回错误
        if (r==null) {
            //空值写进redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            //返回错误信息
            return null;
        }
        //存在写入redis
        this.set(key,r,time,unit);
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public <R,ID> R queryWithLogicalExpire(String keyPrefix,ID id,Class<R>type,Function<ID,R>dbFallback,Long time,TimeUnit unit){
        String key= keyPrefix + id;
        //从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isBlank(json)) {
            //存在直接返回信息
            return null;
        }
        //命中，需要先吧json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //判断是否过期
        if (expireTime.isAfter((LocalDateTime.now()))) {
            //未过期，直接返回店铺信息
            return r;
        }
        //已过期，需要缓存重建
        //实现缓存重建*
        //获取互斥锁
        String lockKey = keyPrefix + id;
        boolean isLock = tryLock(lockKey);
        //判断是否获取锁成功
        if (isLock) {
            //成功就开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //查数据库
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key,r1,time,unit);
                    //写入redis
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        //不成功返回旧信息
        return r;
    }
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
    public <R,ID> R queryWithMutex(String keyPrefix,ID id,Class<R>type,Function<ID,R>dbFallback,Long time,TimeUnit unit){
        String key= keyPrefix + id;
        //从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //存在直接返回信息
            return JSONUtil.toBean(json, type);
        }
        if (json!=null) {
            return null;
        }
        //实现缓存重建*
        //获取互斥锁
        String lockKey=LOCK_SHOP_KEY+id;
        R r=null;
        try {
            boolean isLock = tryLock(lockKey);
            //判断是否获取成功
            if (!isLock) {
                Thread.sleep(50);
                //失败就休眠并重试
                return queryWithMutex(keyPrefix,id,type,dbFallback,time,unit);
            }
            //成功就根据id查询数据库
            r = dbFallback.apply(id);
            //不存在返回错误
            if (r==null) {
                //空值写进redis
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                //返回错误信息
                return null;
            }
            //存在写入redis
            this.set(key,r,time,unit);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放互斥锁
            unLock(lockKey);
        }
        return r;
    }
}
