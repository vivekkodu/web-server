package com.adobe.web.server;

import com.adobe.web.client.WebClient;
import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

/**
 * This class will assign the request from the requestQueue to be handled by
 * the WebClient
 */
public class HttpSchedulerThread implements Runnable {
    private int threadID;
    private Socket socket;
    private static Logger logger = Logger.getLogger(ThreadPoolManager.class
            .getName());

    public HttpSchedulerThread(int i) {
        threadID = i;
    }

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (RequestQueue.httpRequestQueue) {
                    while (RequestQueue.httpRequestQueue.isEmpty()) {
                        RequestQueue.httpRequestQueue.wait();
                    }

                    socket = RequestQueue.httpRequestQueue.poll();
                    System.out.println("Executor Thread" + Thread.currentThread().getName());
                    WebClient client = new WebClient(socket);
                    client.handleClient();
                    RequestQueue.httpRequestQueue.notify();
                }

                socket.setSoTimeout(WebServerConstants.requestTimeOut);
                ThreadInformation.hasRequest[threadID] = true;
                socket.setSoTimeout(WebServerConstants.requestTimeOut);
                try {
                    ThreadInformation.lastSeenAt[threadID] = System.currentTimeMillis();
                    synchronized (ThreadInformation.requestServed) {
                        ThreadInformation.requestServed++;
                    }
                } catch (Exception e) {
                    ThreadInformation.hasRequest[threadID] = false;
                    Thread.interrupted();
                }

                socket.close();
            } catch (IOException e) {
                logger.error("IOException occurred in thread" + e.getMessage());
            } catch (InterruptedException e) {
                logger.error("InterruptedException occurred in thread" + e.getMessage());
            }
        }
    }
}