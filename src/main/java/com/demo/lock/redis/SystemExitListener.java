/* 
 * Copyright chinesejie    
 *    
 */
package com.demo.lock.redis;

/**
 * <p>
 * 系统退出回调类
 * </p>
 *
 * @author chinesejie
 */
public class SystemExitListener {

    private static boolean over = false;

    public static boolean isOver() {
        return over;
    }

    static ExitHandler handler;

    public static void addTerminateListener(ExitHandler exitHandler) {
        handler = exitHandler;
    }

    public static void terminate() {
        over = true;
        new Thread(handler).start();
    }
}
