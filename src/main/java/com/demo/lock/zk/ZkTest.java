/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.demo.lock.zk;

import org.apache.zookeeper.KeeperException;

/**
 * ZkTest
 *
 * @author chinesejie
 */
public class ZkTest {
    public static void main(String[] args) throws KeeperException, InterruptedException {
        DistributedLockUtil util = DistributedLockUtil.getInstance("1");
        util.lock("key1");
    }

}
