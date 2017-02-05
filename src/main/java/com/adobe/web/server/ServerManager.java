package com.adobe.web.server;

import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * It manages the server lifecycle. It also handles server requests.
 */
class ServerManager implements Runnable {
    private ThreadPoolManager threadPoolManager = null;
    private boolean isActive = true;
    public ServerSocket serverSocket;
    private static int requestArrived = 0;
    private static Logger logger = Logger.getLogger(ServerManager.class
            .getName());

    @Override
    public void run() {
        threadPoolManager = new ThreadPoolManager();
        try {
            logger.info("Starting server on port " + WebServerConstants.PORT);
            serverSocket = new ServerSocket(WebServerConstants.PORT, WebServerConstants.QUEUE_SIZE,
                    InetAddress.getByName("localhost"));
        } catch (Exception e) {
            logger.info("Could not start server on port: "
                    + WebServerConstants.PORT + "The port number :"
                    + WebServerConstants.PORT
                    + "may be already in use.Try with other port");
            System.exit(0);
        }

        logger.trace("Server is started on the port " + WebServerConstants.PORT);

        threadPoolManager.initializeThreadPool();

        while (isActive) {
            try {
                Socket socket = serverSocket.accept();
                requestArrived++;

                synchronized (RequestQueue.httpRequestQueue) {
                    while (RequestQueue.httpRequestQueue.size() >= WebServerConstants.QUEUE_SIZE) {
                        RequestQueue.httpRequestQueue.wait();
                    }

                    RequestQueue.httpRequestQueue.add(socket);
                    RequestQueue.httpRequestQueue.notify();
                }
            } catch (Exception e) {
                if (serverSocket.isClosed())
                    System.out.println("Server Stopped");
                // Stop();
                return;
                // System.exit(0);
            }
        }
    }

    public void stopManager() {
        logger.info("Stopping the manager. Total requests served: " + requestArrived);
        threadPoolManager.destroyThreadPool();
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("error in closing the socket" + e.getMessage());
        }

        isActive = false;
        logger.info("Stopped manager");
    }
}