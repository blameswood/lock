/* 
 * Copyright 畅捷通股份有限公司  @ 2015 版权所有    
 *    
 */
package com.demo.lock.zk3;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.KeeperException;

/**
 * <p>
 实现了一个分布式lock后，可以解决多进程之间的同步问题，但设计多线程+多进程的lock控制需求，
 单jvm中每个线程都和zookeeper进行网络交互成本就有点高了，所以基于DistributedLock，实现了
 一个分布式二层锁。
 
大致原理就是ReentrantLock 和 DistributedLock的一个结合。
 
 单jvm的多线程竞争时，首先需要先拿到第一层的ReentrantLock的锁
拿到锁之后这个线程再去和其他JVM的线程竞争锁，最后拿到之后锁之后就开始处理任务。
锁的释放过程是一个反方向的操作，先释放DistributedLock，再释放ReentrantLock。 可以思考一下，
如果先释放ReentrantLock，假如这个JVM ReentrantLock竞争度比较高，一直其他JVM的锁竞争容易
被饿死。
 *</p>
 * @author 洪光华 <br/>
 */
public class DistributedReentrantLock extends DistributedLock{

	
	 private static final String ID_FORMAT     = "Thread[{0}] Distributed[{1}]";  
	    private ReentrantLock       reentrantLock = new ReentrantLock();  
	  
	    public DistributedReentrantLock(String root) {  
	        super(root);  
	    }  
	  
	    public void lock() throws InterruptedException, KeeperException, TimeoutException {  
	        reentrantLock.lock();//多线程竞争时，先拿到第一层锁  
	        super.lock();  
	    }  
	  
	    public boolean tryLock() throws KeeperException {  
	        //多线程竞争时，先拿到第一层锁  
	        return reentrantLock.tryLock() && super.tryLock();  
	    }  
	  
	    public void unlock() throws KeeperException {  
	        super.unlock();  
	        reentrantLock.unlock();//多线程竞争时，释放最外层锁  
	    }  
	  
	    @Override  
	    public String getId() {  
	        return String.format(ID_FORMAT, Thread.currentThread().getId(), super.getId());  
	    }  
	  
	    @Override  
	    public boolean isOwner() {  
	        return reentrantLock.isHeldByCurrentThread() && super.isOwner();  
	    }  
	  

}
