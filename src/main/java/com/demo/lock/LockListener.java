/* 
 * Copyright 畅捷通股份有限公司  @ 2015 版权所有    
 *    
 */
package com.demo.lock;

/**
 * <p>
 * TODO (用一句话描述该文件做什么)   
 *</p>
 * @author 洪光华 <br/>
 */
public interface LockListener {

	public void lockReleased();

	void lockAcquired();

	long getExpire();

	void lockError();

}
