/*
 * Copyright (C) 2016 chinesejie, Inc. All Rights Reserved.
 */
package com.demo.lock.redis;

import org.apache.log4j.Logger;

/**
 * redis 锁测试
 *
 * @author chinesejie
 */
public class RedisLockTest {
    private static final Logger logger = Logger.getLogger(RedisLockTest.class);

    public static void main(String[] args) {
        RedisLockUtil util = RedisLockUtil.getInstance("0");
        RedisLockUtil.LockStat stat = util.lock("key1", 3); // 3s,秒为单位
        if (RedisLockUtil.LockStat.SUCCESS.equals(stat)) {
            logger.info("上锁成功");
        } else if (RedisLockUtil.LockStat.NONEED.equals(stat)) {
            logger.info("锁在你手上,可重入加锁");
        } else {
            logger.info("fxxk,拿不到");
        }
    }
}
