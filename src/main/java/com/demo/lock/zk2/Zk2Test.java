/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.demo.lock.zk2;

/**
 * Zk2Test
 *
 * @author chinesejie
 */
public class Zk2Test {
    public static void main(String[] args) {
        DistributedLock lock = null;
        try {
            lock = new DistributedLock("127.0.0.1:2182", "test");
            lock.lock();
            //do something...
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

}
