/*
 * Copyright 畅捷通股份有限公司 @ 2015 版权所有
 */
package com.demo.lock.zk3;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;


/**
 * <p>
 * TODO (用一句话描述该文件做什么)
 * </p>
 * 
 * @author 洪光华 <br/>
 */
public class DistributedLock {

	private static final byte[] data = { 0x12, 0x34 };
	private static final long DEFAULT_TIMEOUT_PERIOD = 500;
	private ZooKeeper zookeeper = null;
	private final String root; // 根节点路径
	private String id;
	private LockNode idName;
	private String ownerId;
	private String lastChildId;
	private Throwable other = null;
	private KeeperException exception = null;
	private InterruptedException interrupt = null;

	public DistributedLock(String root) {
		this.root = root;
		ensureExists(root);
	}

	/**
	 * 尝试获取锁操作，阻塞式可被中断
	 * @throws TimeoutException 
	 */
	public void lock() throws InterruptedException, KeeperException, TimeoutException {
		// 可能初始化的时候就失败了
		if (exception != null) {
			throw exception;
		}

		if (interrupt != null) {
			throw interrupt;
		}

		if (other != null) {
			throw new NestableRuntimeException(other);
		}

		if (isOwner()) {// 锁重入
			return;
		}

		BooleanMutex mutex = new BooleanMutex();
		acquireLock(mutex);
		mutex.get(DEFAULT_TIMEOUT_PERIOD, TimeUnit.MICROSECONDS);// 阻塞等待值为true
		// mutex.get();

		if (exception != null) {
			throw exception;
		}

		if (interrupt != null) {
			throw interrupt;
		}

		if (other != null) {
			throw new NestableRuntimeException(other);
		}
	}

	/**
	 * 尝试获取锁对象, 不会阻塞
	 * 
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public boolean tryLock() throws KeeperException {
		// 可能初始化的时候就失败了
		if (exception != null) {
			throw exception;
		}

		if (isOwner()) {// 锁重入
			return true;
		}

		acquireLock(null);

		if (exception != null) {
			throw exception;
		}

		if (interrupt != null) {
			Thread.currentThread().interrupt();
		}

		if (other != null) {
			throw new NestableRuntimeException(other);
		}

		return isOwner();
	}

	/**
	 * 释放锁对象
	 */
	public void unlock() throws KeeperException {
		if (id != null) {
			try {
				zookeeper.delete(root + "/" + id, -1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (KeeperException.NoNodeException e) {
				// do nothing
			} finally {
				id = null;
			}
		} else {
			// do nothing
		}
	}

	private void ensureExists(final String path) {
		try {
			Stat stat = zookeeper.exists(path, false);
			if (stat != null) {
				return;
			}

			zookeeper.create(path, data, null, CreateMode.PERSISTENT);
		} catch (KeeperException e) {
			exception = e;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			interrupt = e;
		}
	}

	/**
	 * 返回锁对象对应的path
	 */
	public String getRoot() {
		return root;
	}

	/**
	 * 判断当前是不是锁的owner
	 */
	public boolean isOwner() {
		return id != null && ownerId != null && id.equals(ownerId);
	}

	/**
	 * 返回当前的节点id
	 */
	public String getId() {
		return this.id;
	}

	// ===================== helper method =============================

	/**
	 * 执行lock操作，允许传递watch变量控制是否需要阻塞lock操作
	 */
	private Boolean acquireLock(final BooleanMutex mutex) {
		try {
			do {
				if (id == null) {// 构建当前lock的唯一标识
					long sessionId =0;// ((Object) zookeeper).getDelegate().getSessionId();
					String prefix = "x-" + sessionId + "-";
					// 如果第一次，则创建一个节点
					String path = zookeeper.create(root + "/" + prefix, data, null, CreateMode.EPHEMERAL_SEQUENTIAL);
					int index = path.lastIndexOf("/");
					id =    path.substring(index+1);//StringUtils.substring(path, index + 1);
					idName = new LockNode(id);
				}

				if (id != null) {
					List<String> names = zookeeper.getChildren(root, false);
					if (names.isEmpty()) {
						id = null;// 异常情况，重新创建一个
					} else {
						// 对节点进行排序
						SortedSet<LockNode> sortedNames = new TreeSet<LockNode>();
						for (String name : names) {
							sortedNames.add(new LockNode(name));
						}

						if (sortedNames.contains(idName) == false) {
							id = null;// 清空为null，重新创建一个
							continue;
						}

						// 将第一个节点做为ownerId
						ownerId = sortedNames.first().getName();
						if (mutex != null && isOwner()) {
							mutex.set(true);// 直接更新状态，返回
							return true;
						} else if (mutex == null) {
							return isOwner();
						}

						SortedSet<LockNode> lessThanMe = sortedNames.headSet(idName);
						if (!lessThanMe.isEmpty()) {
							// 关注一下排队在自己之前的最近的一个节点
							LockNode lastChildName = lessThanMe.last();
							lastChildId = lastChildName.getName();
							// 异步watcher处理
							Stat stat = zookeeper.exists(root + "/" + lastChildId, new Watcher() {

								public void process(WatchedEvent event) {
									acquireLock(mutex);
								}

							});

							if (stat == null) {
								acquireLock(mutex);// 如果节点不存在，需要自己重新触发一下，watcher不会被挂上去
							}
						} else {
							if (isOwner()) {
								mutex.set(true);
							} else {
								id = null;// 可能自己的节点已超时挂了，所以id和ownerId不相同
							}
						}
					}
				}
			} while (id == null);
		} catch (KeeperException e) {
			exception = e;
			if (mutex != null) {
				mutex.set(true);
			}
		} catch (InterruptedException e) {
			interrupt = e;
			if (mutex != null) {
				mutex.set(true);
			}
		} catch (Throwable e) {
			other = e;
			if (mutex != null) {
				mutex.set(true);
			}
		}

		if (isOwner() && mutex != null) {
			mutex.set(true);
		}
		return Boolean.FALSE;
	}
}
