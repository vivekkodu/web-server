package com.adobe.web.readers;

import com.adobe.web.server.MalformedRequestException;
import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URLDecoder;
import java.util.Hashtable;

/**
 * Base class to process http requests.
 */
public abstract class HttpRequestProcessor {

    private static Logger logger = Logger
            .getLogger(HttpRequestProcessor.class.getName());
    protected static BufferedInputStream inputStream;
    protected static BufferedOutputStream outputStream;
    protected static BufferedWriter charStreamOutput;

    /**
     * this is the constructor for intializing the streams connected to client
     *
     * @param bufferedByteInputStream  byte input stream
     * @param bufferedByeOutputStream  byte output stream
     * @param bufferedCharOutputStream charater output stream
     */
    protected HttpRequestProcessor(
            BufferedInputStream bufferedByteInputStream,
            BufferedOutputStream bufferedByeOutputStream,
            BufferedWriter bufferedCharOutputStream) {
        inputStream = bufferedByteInputStream;
        outputStream = bufferedByeOutputStream;
        charStreamOutput = bufferedCharOutputStream;
    }

    /**
     * Gets the header key and value.
     * @return Map containing request header key and value
     * @throws IOException Exception in case of input/output failures
     * @throws MalformedRequestException Exception if request is malformed.
     */
    public static Hashtable<String, String> getAllHeaders() throws IOException, MalformedRequestException {
        String requestHeaders = HeaderHandler.headerReader(inputStream);
        Hashtable<String, String> headerPropertyMap = HeaderHandler
                .getclientHeadersinHashTable(requestHeaders);
        return headerPropertyMap;
    }

    /**
     * It decodes the input uri
     * @param requestURI request uri
     * @return null if uri can't be decoded, else decoded uri
     * @throws IOException exception during input/output operation
     */
    public static String decodeURI(String requestURI) throws IOException {
        String decodedURI = null;
        try {
            decodedURI = URLDecoder.decode(requestURI,
                    WebServerConstants.ENCODING);
        } catch (UnsupportedEncodingException e2) {
            logger.error("request uri could not decoded . " + e2.getMessage());
            Reader.serverFormattedResponseToClient("500",
                    "Internal Server Error", "url could not be decoded",
                    charStreamOutput, outputStream, "close");
        }

        return decodedURI;
    }

    /**
     * Creates the file path for the file which has to be deleted.
     * @param requestUri Client requset uri
     * @return Return absolute path for the file which is requested to be deleted.
     */
    protected static String getAbsoluteFilePath(String requestUri) {
        String resourcePath = null;
        StringBuffer outputResource = new StringBuffer(50);
        if (requestUri.equals("/")) {
            resourcePath = WebServerConstants.HOSTPATH + File.separator + "index.html";
            return resourcePath;
        } else {
            String pathList[] = requestUri.split(WebServerConstants.URISeparator);
            for (int i = 0; i < pathList.length; i++) {
                if (pathList[i] != null && pathList[i].length() > 0)
                    outputResource.append(File.separator + pathList[i]);
            }

            resourcePath = WebServerConstants.HOSTPATH + outputResource.toString();
        }

        return resourcePath;
    }

}
