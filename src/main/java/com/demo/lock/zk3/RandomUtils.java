/* 
 * Copyright chinesejie    
 *    
 */
package com.demo.lock.zk3;

import java.util.Random;

/**
 * <p>
 * TODO (用一句话描述该文件做什么)   
 *</p>
 * @author chinesejie <br/>
 */
public class RandomUtils {
	private static Random  random  = new Random(System.currentTimeMillis());
	public static int nextInt(int i) {
		return random.nextInt(i);
        }
	

}
