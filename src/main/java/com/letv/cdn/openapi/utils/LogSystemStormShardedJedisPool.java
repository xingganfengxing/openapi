/*
 * Copyright  2015. letv.com All Rights Reserved. 
 * Application : openapi 
 * Class Name  : logSystemStormShardedJedisPool.java 
 * Date Created: 2015年4月14日 
 * Author      : chenyuxin 
 * 
 * Revision History 
 * 2015年4月14日 下午3:03:35 Amend By chenyuxin 
 */
package com.letv.cdn.openapi.utils;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

/**
 * TODO:日志系统storm集群的redis
 * 
 * @author chenyuxin
 * @createDate 2015年4月14日
 */
public class LogSystemStormShardedJedisPool{
    
    /**存储在redis里的域名标识前缀*/
    public static final String KEY_DOMAINTAG_PREFIX = "dm_";
    
    /**日志系统storm集群的共享redispool*/
    private static ShardedJedisPool pool;
    
    /**
     * 创建日志系统storm集群的共享redispool
     * 
     * @method: DomainApplyService  getShardedJedisPool
     */
    private static void bulidLogSystemStormShardedJedisPool(){
        JedisPoolConfig config = new JedisPoolConfig();// Jedis池配置
        String[] iparr = Env.get("logsystem_storm_redis_address").split(",");
        List<JedisShardInfo> jdsInfoList = new ArrayList<JedisShardInfo>();
        for (String ip : iparr) {
            jdsInfoList.add(new JedisShardInfo(ip));
        }
        pool = new ShardedJedisPool(config, jdsInfoList);
    }
    
    public static ShardedJedisPool getLogSystemStormShardedJedisPool(){
        if(pool == null){
            bulidLogSystemStormShardedJedisPool();
        }
        return pool;
    }
    
}
