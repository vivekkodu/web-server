package com.adobe.web.server;

import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.util.Properties;

/**
 * This is the entry point of the server. It will start a thread pool and create sockets with the client.
 */
public class HttpWebServer {

    private static Logger logger = Logger.getLogger(HttpWebServer.class
            .getName());
    private static ServerManager serverManager = null;

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
    }

    /**
     * It stops the server.
     *
     * @throws InterruptedException Exception on interruptions.
     */
    public void stopServer() throws InterruptedException {
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
            webServer.stopServer();
        } catch (InterruptedException e) {
            logger.error("server could not be stopped properly .. exiting thread");
            return;
        }
    }
}
