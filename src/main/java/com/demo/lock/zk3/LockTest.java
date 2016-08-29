/*
 * Copyright 畅捷通股份有限公司 @ 2015 版权所有
 */
package com.demo.lock.zk3;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.apache.zookeeper.KeeperException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class LockTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    String dir = "/res/bb";

    @Test
    public void test_lock() {
        ExecutorService exeucotr = Executors.newCachedThreadPool();
        final int count = 50;

        final CountDownLatch latch = new CountDownLatch(count);
        final DistributedLock[] nodes = new DistributedLock[count];
        for (int i = 0; i < count; i++) {
            final DistributedLock node = new DistributedLock(dir);
            nodes[i] = node;
            exeucotr.submit(new Runnable() {

                public void run() {
                    try {
                        Thread.sleep(1000);
                        node.lock(); // 获取锁
                        Thread.sleep(100 + RandomUtils.nextInt(100));

                        System.out.println("id: " + node.getId() + " is leader: " + node.isOwner());
                    } catch (InterruptedException e) {
                        fail();
                    } catch (KeeperException e) {
                        fail();
                    } catch (TimeoutException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                        try {
                            node.unlock();
                        } catch (KeeperException e) {
                            fail();
                        }
                    }

                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            fail();
        }

        exeucotr.shutdown();
    }

    @Test
    public void test_lock2() {
        ExecutorService exeucotr = Executors.newCachedThreadPool();
        final int count = 50;
        final CountDownLatch latch = new CountDownLatch(count);

        final DistributedReentrantLock lock = new DistributedReentrantLock(dir); // 单个锁
        for (int i = 0; i < count; i++) {
            exeucotr.submit(new Runnable() {

                public void run() {
                    try {
                        Thread.sleep(1000);
                        lock.lock();
                        Thread.sleep(100 + RandomUtils.nextInt(100));

                        System.out.println("id: " + lock.getId() + " is leader: " + lock.isOwner());
                    } catch (InterruptedException e) {
                        fail();
                    } catch (KeeperException e) {
                        fail();
                    } catch (TimeoutException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                        try {
                            lock.unlock();
                        } catch (KeeperException e) {
                            fail();
                        }
                    }

                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            fail();
        }

        exeucotr.shutdown();
    }

    public void fail() {

    }
}
