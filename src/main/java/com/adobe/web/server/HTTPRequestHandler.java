package com.adobe.web.server;

import com.adobe.web.client.WebClient;
import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/**
 * It handles all the incoming requests. It also maintains the threadpool and the request queue.
 */
public class HttpRequestHandler {

    private static Thread[] httpWebServerThread = new Thread[WebServerConstants.THREAD_POOL_SIZE];
    private static Logger logger = Logger.getLogger(HttpRequestHandler.class
            .getName());
    private static boolean[] hasRequest = new boolean[WebServerConstants.THREAD_POOL_SIZE];
    private static long[] lastSeenAt = new long[WebServerConstants.THREAD_POOL_SIZE];
    private static Integer requestServed = 0;
    public static Queue<Socket> httpRequestQueue = new LinkedList<Socket>();

    public class HttpRequestSchedulerThread implements Runnable {
        @SuppressWarnings("deprecation")
        public void run() {
            long current;
            try {
                while (true) {
                    current = System.currentTimeMillis();
                    for (int i = 0; i < WebServerConstants.THREAD_POOL_SIZE; i++) {
                        if (hasRequest[i]
                                && (current - lastSeenAt[i]) > WebServerConstants.requestTimeOut) {
                            httpWebServerThread[i].stop(new Exception());
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

    public HttpRequestHandler() {

    }

    /**
     * This class will assign the request from the requestQueue to be handled by
     * the WebClient
     */
    public class HttpSchedulerThread implements Runnable {
        private int threadID;
        private volatile boolean exit = false;
        Socket socket;

        public HttpSchedulerThread(int i) {
            threadID = i;
        }

        @Override
        public void run() {
            while (!exit) {
                try {
                    synchronized (httpRequestQueue) {
                        while (httpRequestQueue.isEmpty()) {
                            httpRequestQueue.wait();
                        }

                        socket = httpRequestQueue.poll();
                        System.out.println("Executor Thread" + Thread.currentThread().getName());
                        WebClient client = new WebClient(socket);
                        client.handleClient();
                        httpRequestQueue.notify();
                    }

                    socket.setSoTimeout(WebServerConstants.requestTimeOut);
                    hasRequest[threadID] = true;
                    socket.setSoTimeout(WebServerConstants.requestTimeOut);
                    try {
                        lastSeenAt[threadID] = System.currentTimeMillis();
                        synchronized (requestServed) {
                            requestServed++;
                        }
                    } catch (Exception e) {
                        hasRequest[threadID] = false;
                        Thread.interrupted();
                    }

                    socket.close();
                } catch (IOException e) {
                } catch (InterruptedException e) {
                    logger.error("the error has occured" + e.getMessage());
                }
            }
        }

        public void stop() {
            exit = true;
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
