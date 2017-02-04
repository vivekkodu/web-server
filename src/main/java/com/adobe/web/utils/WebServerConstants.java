package com.adobe.web.utils;

import java.io.File;

/**
 * Constants for the service.
 */
public class WebServerConstants {

    public static String CRLF = "\r\n";
    public static String ENCODING = "UTF-8";
    public static String HOSTPATH = "D:\\Projects\\HttpServer\\web-server\\src\\test\\resources\\userFiles";
    public static final String URISeparator = "/";
    public static String UPLOAD_PATH = "directory";
    public static int THREAD_POOL_SIZE = 50;
    public static String PROPERTIES_PATH = "config" + File.separator + "config.properties";
    public static String PROPERTIES_PATH_LOG4J = "config" + File.separator + "log4j.properties";
    public static String HOSTNAME = "localhost";
    public static int QUEUE_SIZE = 100;
    public static final String HttpHeadersEncoding = "ISO8859_1";
    public static final int ServerSocketSoTimeout = 20000000;
    public static int PORT = 9191;
    public static int requestTimeOut = 1000;


}
