package com.hiwuyue.leaderelection.utils;

import java.util.concurrent.TimeUnit;

public class ThreadUtil {
    public static void sleepIgnoreInterrupt(long time, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(time));
        } catch (InterruptedException ignored) {
        }
    }
}
