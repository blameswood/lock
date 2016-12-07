/*
 * Copyright chinesejie
 */
package com.demo.lock.redis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;

import com.demo.lock.LockListener;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.util.SafeEncoder;

/**
 * <p>
 * AbstractLockObserver 的扩展,这是一个 锁的观察者,不对跑 run函数,对各种 锁请求[lockMap] 进行tryLock操作.
 * 无论成功还是失败,都会把相应的LockListener 移除.
 * </p>
 *
 * @author chinesejie
 */
public class LockObserver extends AbstractLockObserver implements Runnable {
    private Jedis client;
    private Object mutex = new Object();
    private Map<String, LockListener> lockMap = new ConcurrentHashMap();
    private boolean stoped = false;
    private long interval = 500;
    private boolean terminated = false;
    private CountDownLatch doneSignal = new CountDownLatch(1);

    // 默认schema就是0. 一般redis有16个db:[0,15],可以用SELECT 1  切换
    public LockObserver(String schema) {

        client = RedisUtil.getJedis();
        if (StringUtils.isEmpty(schema)) {
            client.select(Integer.valueOf(0));
        } else {
            client.select(Integer.valueOf(schema));
        }
        SystemExitListener.addTerminateListener(new ExitHandler() {
            public void run() {
                stoped = true;
                try {
                    doneSignal.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void addLockListener(String key, LockListener listener) {
        if (terminated) {
            listener.lockError();
            return;
        }
        synchronized(mutex) {
            lockMap.put(key, listener);
        }
    }

    public void removeLockListener(String key) {
        synchronized(mutex) {
            lockMap.remove(key);
        }
    }

    @Override
    public void run() {
        while (!terminated) {
            long p1 = System.currentTimeMillis();
            Map<String, LockListener> clone = new HashMap();
            synchronized(mutex) {
                clone.putAll(lockMap);
            }
            Set<String> keyset = clone.keySet();
            if (keyset.size() > 0) {
                //                ConnectionFactory.setSingleConnectionPerThread(keyset.size());
                for (String key : keyset) {
                    LockListener ll = clone.get(key);
                    try {
                        if (tryLock(key, ll.getExpire())) {
                            ll.lockAcquired();
                            removeLockListener(key);
                        }
                    } catch (Exception e) {
                        ll.lockError();
                        removeLockListener(key);
                    }
                }
                //                ConnectionFactory.releaseThreadConnection();
            } else {
                if (stoped) {
                    terminated = true;
                    doneSignal.countDown();
                    return;
                }
            }
            try {
                long p2 = System.currentTimeMillis();
                long cost = p2 - p1;
                if (cost <= interval) {
                    Thread.sleep(interval - cost);
                } else {
                    Thread.sleep(interval * 2);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 超时时间单位为s!!!
     *
     * @param key
     * @param expireInSecond
     *
     * @return
     */
    public boolean tryLock(final String key, final long expireInSecond) {
        if (terminated) {
            return false;
        }
        final long tt = System.currentTimeMillis();
        final long expire = expireInSecond * 1000;
        final Long ne = tt + expire;

        // 事务开始
        final Transaction transaction = client.multi();
        transaction.setnx(key, String.valueOf(ne));
        transaction.get(SafeEncoder.encode(key));
        List<Object> mm = transaction.exec();
        // 事务结束
        Long res = (Long) mm.get(0);
        if (new Long(1).equals(res)) {
            return true;
        } else {
            byte[] bb = (byte[]) mm.get(1);
            Long ex = java.lang.Long.parseLong(new String(bb));
            ;
            if (ex == null || tt > ex) {
                String oValue = client.get(key);
                Long old = Long.parseLong(oValue);
                client.set(key, String.valueOf(new Long(ne + 1)));
                if (old == null || (ex == null && old == null) || (ex != null && ex.equals(old))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void unLock(String key) {
        client.del(key);
    }
}
