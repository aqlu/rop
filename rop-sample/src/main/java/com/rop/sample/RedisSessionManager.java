package com.rop.sample;

import com.rop.session.Session;
import com.rop.session.SessionManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by aqlu on 14-3-22.
 */
public class RedisSessionManager implements SessionManager {

    private RedisTemplate redisTemplate;

    private int expiredBySecond = 300;

    private static Map<String, Long> lastTimeMap = new ConcurrentHashMap<String, Long>();

    @Override
    public void addSession(String sessionId, Session session) {
        redisTemplate.opsForValue().set(sessionId, session);
        redisTemplate.expireAt(sessionId, getSessionExpireDate());
        lastTimeMap.put(sessionId, System.currentTimeMillis());
    }

    @Override
    public Session getSession(String sessionId) {
        Session session = (Session) redisTemplate.opsForValue().get(sessionId);
        Long lastTime = lastTimeMap.get(sessionId);

        // 每隔5分钟重新刷新一次Redis中Session的过期时间
        if (lastTime != null && System.currentTimeMillis() - lastTime > 5 * 60 * 1000) {
            redisTemplate.expireAt(sessionId, getSessionExpireDate());
            lastTimeMap.put(sessionId, System.currentTimeMillis());
        }
        return session;
    }

    @Override
    public void removeSession(String sessionId) {
        redisTemplate.delete(sessionId);
        lastTimeMap.remove(sessionId);
    }

    /**
     * 登录一个小时 session自动失效
     * 
     * @return
     */
    private Date getSessionExpireDate() {
        return new Date(getCurrentTimeMillis() + expiredBySecond * 1000);
    }

    /**
     * 取redis当前时间
     * 
     * @return
     */
    public long getCurrentTimeMillis() {
        RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
        try {
            Jedis jedis = (Jedis) conn.getNativeConnection();
            Object resultObj = jedis.eval("return redis.call('time')[1]");
            return Long.parseLong(resultObj.toString()) * 1000;
        } finally {
            conn.close();
        }
    }

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public int getExpiredBySecond() {
        return expiredBySecond;
    }

    public void setExpiredBySecond(int expiredBySecond) {
        this.expiredBySecond = expiredBySecond;
    }

}