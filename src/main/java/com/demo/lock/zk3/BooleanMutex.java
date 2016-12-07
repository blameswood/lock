/*
 * Copyright chinesejie
 */
package com.demo.lock.zk3;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * <p>
http://agapple.iteye.com/blog/1183972
背景

  最近一个月都在做项目，我主要负责分布式任务的调度的功能，需要实现一个分布式的授权控制。
  具体的需求：
  1.  首先管理员启动整个任务，并设置执行权限
  2.  工作节点收到消息后就会创建对应的线程，并开始执行任务(任务都是由一个管理员进行分配)
  3.  运行过程中管理员需要临时中断某个任务，需要设置一个互斥信号，此时对应的工作节点都需要被阻塞，注意不是完全销毁
分析

 先抛开分布式通讯这一块，首先从单个jvm如何实现进行分析, 简单点来说：
 在单jvm中就是两种线程，一个为manager，另一种为worker。1:n的对应关系，manager可以随时挂起worker的所有线程，而worker线程互不干扰。
 
 咋一看，会觉得是一个比较典型的读写锁的应用场景，读写锁特性：
当读写锁是写加锁状态时, 在这个锁被解锁之前, 所有试图对这个锁加锁的线程都会被阻塞.
当读写锁在读加锁状态时, 所有试图以读模式对它进行加锁的线程都可以得到访问权, 但是如果线程希望以写模式对此锁进行加锁, 它必须直到知道所有的线程释放锁.
使用读写锁实现这样的功能会存在一个问题，就是对应的写锁是没有抢占权，比如当前有读锁未释放时，写锁一直会被阻塞。而项目的需求是，manager是个领导，它可以不用排队，随时打断你。
除此之外，整个worker线程操作会是一个跨方法，跨类的复杂实现。通过lock方式实现，异常稍微处理不好，很容易造成锁未释放，导致manager一直拿不到对应的锁操作。而且worker中本省会使用一些lock操作，容易造成死锁
 
总结一下： 
需要的是一个类似于信号量的PV控制
具有的读写锁的，读线程可以不互相影响，写线程拥有最高的抢占权，可以不理会读线程是否在操作
支持线程中断 (worker线程需要允许cancel)
因此本文的互斥信号(BooleanMutex)就应运而生，它是信号量(Semaphore)的一个变种，加入了读锁的特性。比如在状态为1时可以一直得到响应，对应的P操作不会消费对应的资源
实现

  基于jdk 1.5之后的concurrent的AQS，实现了一个自己的互斥信号控制。 A.Q.S的可以看我的另一篇文章：jdk中cocurrent下的AbstractQueuedSynchronizer理解记录
 网址:http://agapple.iteye.com/blog/966490
 * </p>
 * 
 * @author chinesejie <br/>
 */
public class BooleanMutex {

	private Sync sync;

	public BooleanMutex() {
		sync = new Sync();
		set(false);
	}

	public BooleanMutex(Boolean mutex) {
		sync = new Sync();
		set(mutex);
	}

	/**
	 * 阻塞等待Boolean为true
	 * 
	 * @throws InterruptedException
	 */
	public void get() throws InterruptedException {
		sync.innerGet();
	}

	/**
	 * 阻塞等待Boolean为true,允许设置超时时间
	 * 
	 * @param timeout
	 * @param unit
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public void get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		sync.innerGet(unit.toNanos(timeout));
	}

	/**
	 * 重新设置对应的Boolean mutex
	 * 
	 * @param mutex
	 */
	public void set(Boolean mutex) {
		if (mutex) {
			sync.innerSetTrue();
		} else {
			sync.innerSetFalse();
		}
	}

	public boolean state() {
		return sync.innerState();
	}

	/**
	 * Synchronization control for BooleanMutex. Uses AQS sync state to
	 * represent run status
	 */
	private final class Sync extends AbstractQueuedSynchronizer {
		private static final long serialVersionUID = -7828117401763700385L;

		/** State value representing that TRUE */
		private static final int TRUE = 1;
		/** State value representing that FALSE */
		private static final int FALSE = 2;

		private boolean isTrue(int state) {
			return (state & TRUE) != 0;
		}

		/**
		 * 实现AQS的接口，获取共享锁的判断
		 */
		protected int tryAcquireShared(int state) {
			// 如果为true，直接允许获取锁对象 读锁
			// 如果为false，进入阻塞队列，等待被唤醒 写锁
			return isTrue(getState()) ? 1 : -1;
		}

		/**
		 * 实现AQS的接口，释放共享锁的判断
		 */
		protected boolean tryReleaseShared(int ignore) {
			// 始终返回true，代表可以release
			return true;
		}

		boolean innerState() {
			return isTrue(getState());
		}

		void innerGet() throws InterruptedException {
			acquireSharedInterruptibly(0);
		}

		void innerGet(long nanosTimeout) throws InterruptedException, TimeoutException {
			if (!tryAcquireSharedNanos(0, nanosTimeout))
				throw new TimeoutException();
		}

		void innerSetTrue() {
			for (;;) {
				int s = getState();
				if (s == TRUE) {
					return; // 直接退出
				}
				if (compareAndSetState(s, TRUE)) {// cas更新状态，避免并发更新true操作
					releaseShared(0);// 释放一下锁对象，唤醒一下阻塞的Thread
				}
			}
		}

		void innerSetFalse() {
			for (;;) {
				int s = getState();
				if (s == FALSE) {
					return; // 直接退出
				}
				if (compareAndSetState(s, FALSE)) {// cas更新状态，避免并发更新false操作
					setState(FALSE);
				}
			}
		}

	}

}
