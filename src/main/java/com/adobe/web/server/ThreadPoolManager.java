package com.adobe.web.server;

import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;

/**
 * It handles all the incoming requests. It also maintains the threadpool and the request queue.
 */
public class ThreadPoolManager {

    private static Thread[] httpWebServerThread = new Thread[WebServerConstants.THREAD_POOL_SIZE];
    private static Logger logger = Logger.getLogger(ThreadPoolManager.class
            .getName());

    public class HttpRequestSchedulerThread implements Runnable {
        @SuppressWarnings("deprecation")
        public void run() {
            long current;
            try {
                while (true) {
                    current = System.currentTimeMillis();
                    for (int i = 0; i < WebServerConstants.THREAD_POOL_SIZE; i++) {
                        if (ThreadInformation.hasRequest[i]
                                && (current - ThreadInformation.lastSeenAt[i]) > WebServerConstants.requestTimeOut) {
                            // But this will close the thread completely.
                            //ToDO: Need to dissociate task with thread.
                            httpWebServerThread[i].interrupt();
                        }
                    }

                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                logger.error("an error has occured while it is interrupted"
                        + e.getMessage());
            }
        }
    }

    /**
     * This will initialize the thread pool.
     */
    public void initializeThreadPool() {

        for (int i = 0; i < WebServerConstants.THREAD_POOL_SIZE; i++) {
            httpWebServerThread[i] = new Thread(new HttpSchedulerThread(i));
            httpWebServerThread[i].start();
        }

        // starting the schduler thread that is scheduling the threads in the
        // threadPool

        Thread httpRequestSchedulerThread = new Thread(
                new HttpRequestSchedulerThread());
        httpRequestSchedulerThread.start();
    }

    /**
     * This will destroy the thread pool.
     */
    public void destroyThreadPool() {
        for (int i = 0; i < WebServerConstants.THREAD_POOL_SIZE; i++) {
            httpWebServerThread[i].stop();
        }
    }
}
