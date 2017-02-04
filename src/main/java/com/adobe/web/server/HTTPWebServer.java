package com.adobe.web.server;

import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * This is the entry point of the server. It will start a thread pool and create sockets with the client.
 */
public class HttpWebServer {

    private static Logger logger = Logger.getLogger(HttpWebServer.class
            .getName());
    private static ServerManager serverManager = null;
    private static int requestArrived = 0;

    /**
     * It read configuration file and initialize the server properties.
     *
     * @param serverPropertyFile File which contains the server properties.
     * @throws IOException Input or output failures.
     */
    public void serverInitialiation(File serverPropertyFile)
            throws IOException {
        logger.info("Initializing server properties by reading from properties file");
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream(serverPropertyFile);
        properties.load(inputStream);
        WebServerConstants.PORT = Integer.parseInt(properties
                .getProperty("Port"));
        WebServerConstants.HOSTNAME = properties.getProperty("HostName");
        WebServerConstants.UPLOAD_PATH = properties.getProperty("UploadPath");
        WebServerConstants.HOSTPATH = properties.getProperty("HostPath");
        WebServerConstants.THREAD_POOL_SIZE = Integer.parseInt(properties
                .getProperty("ThreadPoolSize"));
        WebServerConstants.requestTimeOut = Integer.parseInt(properties.getProperty("requestTimeOut"));
        WebServerConstants.QUEUE_SIZE = Integer.parseInt(properties.getProperty("requestQueueMaxSize"));
    }

    /**
     * It starts the server and keep listening to the incoming requests.
     *
     * @throws InterruptedException Exception on interruptions.
     */
    public void startServerAndListen() throws InterruptedException {
        serverManager = new ServerManager();
        Thread serverManagerThread = new Thread(serverManager);
        serverManagerThread.start();
        serverManagerThread.interrupt();
    }

    /**
     * It stops the server.
     *
     * @throws InterruptedException Exception on interruptions.
     */
    public void serverStop() throws InterruptedException {
        serverManager.stopManager();
        System.exit(0);
    }

    /**
     * Entry point of the process.
     *
     * @param args Array of input parameters.
     */
    public static void main(String args[]) {
        PropertyConfigurator.configure("log4j.properties");
        HttpWebServer webServer = new HttpWebServer();
        try {
            webServer.serverInitialiation(new File("config.properties"));

        } catch (FileNotFoundException fileNotFoundException) {
            logger.error("properties file could not be found"
                    + fileNotFoundException.getMessage());
            return;
        } catch (IOException ioException) {
            logger.error("properties file reading is causing an error"
                    + ioException.getMessage());
            return;
        }

        try {
            webServer.startServerAndListen();
        } catch (InterruptedException interruptedException) {

            logger.error("thread is interrupted"
                    + interruptedException.getMessage());
            return;
        }

        logger.info("enter e for stopping the server");
        try {
            while ((char) System.in.read() != 'e')
                ;
        } catch (IOException e) {
            logger.error("key pressed cant be read");

        }

        try {
            webServer.serverStop();
        } catch (InterruptedException e) {
            logger.error("server could not be stopped properly .. exiting thread");
            return;
        }
    }

    /**
     * It manages the server lifecycle. It also handles server requests.
     */
    public class ServerManager implements Runnable {
        private HttpRequestHandler handler = null;
        private boolean isActive = true;
        public ServerSocket serverSocket;

        @Override
        public void run() {
            handler = new HttpRequestHandler();
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

            handler.initializeThreadPool();

            while (isActive) {
                try {
                    Socket socket = serverSocket.accept();
                    requestArrived++;

                    synchronized (handler.httpRequestQueue) {
                        while (handler.httpRequestQueue.size() >= WebServerConstants.QUEUE_SIZE) {
                            handler.httpRequestQueue.wait();
                        }

                        handler.httpRequestQueue.add(socket);
                        handler.httpRequestQueue.notify();
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
            logger.info("Stopping the manager");
            handler.destroyThreadPool();
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("error in closing the socket" + e.getMessage());
            }

            isActive = false;
            logger.info("Stopped manager");
        }
    }
}
