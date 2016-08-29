/* 
 * Copyright 畅捷通股份有限公司  @ 2015 版权所有    
 *    
 */
package com.demo.lock.redis;

import com.demo.lock.LockListener;

/**
 * <p>
 * TODO (用一句话描述该文件做什么)   
 *</p>
 * @author 洪光华 <br/>
 */
public class AbstractLockObserver implements Runnable{

	@Override
        public void run() {
	        // TODO Auto-generated method stub
	        
        }

	public boolean tryLock(String key, long expire) {
	        // TODO Auto-generated method stub
	        return false;
        }

	public void addLockListener(String key, LockListener locker) {
	        // TODO Auto-generated method stub
	        
        }

	public void unLock(String key) {
	        // TODO Auto-generated method stub
	        
        }

	public void removeLockListener(String key) {
	        // TODO Auto-generated method stub
	        
        }

}
