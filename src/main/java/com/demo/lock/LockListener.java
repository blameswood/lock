/* 
 * Copyright chinesejie    
 *    
 */
package com.demo.lock;

/**
 * <p>
 * lock 监听器
 * 1. 锁释放 要干么
 * 2. 得到锁 要干么
 * 3. 得到设置的过期时间
 * 4. 错误了 要干么
 * </p>
 *
 * @author chinesejie <br/>
 */
public interface LockListener {

    public void lockReleased();

    void lockAcquired();

    long getExpire();

    void lockError();

}
