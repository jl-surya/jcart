package config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AsyncExecutor provides a shared thread pool for handling asynchronous operations.
 * 
 * Features:
 * - Core pool size: 480 threads
 * - Maximum pool size: 500 threads
 * - Keep-alive time: 60 seconds
 * - Work queue capacity: 1000 tasks
 * - Rejection policy: Abort (throws exception when overloaded)
 */
public class AsyncExecutor {

    /**
     * Shared thread pool executor for async request processing.
     * Configured for high concurrency with overflow protection.
     */
    public static final ThreadPoolExecutor EXECUTOR =
            new ThreadPoolExecutor(
                    480,                                 // corePoolSize
                    500,                                 // maximumPoolSize
                    60L,                                 // keepAliveTime
                    TimeUnit.SECONDS,                    // timeUnit
                    new ArrayBlockingQueue<>(1000),      // workQueue
                    new ThreadPoolExecutor.AbortPolicy() // rejectionHandler
            );

    static {
        EXECUTOR.allowCoreThreadTimeOut(true);
    }
}