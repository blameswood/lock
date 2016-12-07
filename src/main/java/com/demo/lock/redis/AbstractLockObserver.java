/* 
 * Copyright chinesejie    
 *    
 */
package com.demo.lock.redis;

import com.demo.lock.LockListener;

/**
 * <p>
 * lock 观察者接口,它是一个线程
 * </p>
 *
 * @author chinesejie
 */
public class AbstractLockObserver implements Runnable {

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
