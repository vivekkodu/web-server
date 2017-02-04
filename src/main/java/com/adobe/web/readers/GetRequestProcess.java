package com.adobe.web.readers;

import com.adobe.web.server.MalformedRequestException;
import com.adobe.web.utils.ResponseCodeParams;
import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Hashtable;

/**
 * It handles all the get requests for the server.
 */
public class GetRequestProcess extends HttpRequestProcessor {

    public Hashtable<String, String> getFields;
    private static Logger logger = Logger.getLogger(GetRequestProcess.class
            .getName());
    private static String connectionStatus;

    public GetRequestProcess(BufferedInputStream bufferedByteInputStream,
                             BufferedOutputStream bufferedByeOutputStream,
                             BufferedWriter bufferedCharOutputStream) {

        super(bufferedByteInputStream, bufferedByeOutputStream,
                bufferedCharOutputStream);
    }

    /**
     * It handles the get requests by processing headers and
     * returning the responses.
     *
     * @param requestUri Request uri
     * @throws IOException Throws if exception occurs in input or output
     * @throws MalformedRequestException Throws if request is malformed
     */
    public void handleGetRequest(String requestUri) throws IOException,
            MalformedRequestException {

        Hashtable<String, String> getHeaders = this.getAllHeaders();
        connectionStatus = getHeaders.get("Connection");
        String decodedUri = decodeURI(requestUri);
        if (decodedUri == null) {
            //ToDo: Need to return proper formatted response
            logger.error("the decoded uri is null");
            return;
        }

        String resourcePath = getAbsoluteFilePath(decodedUri);
        File file = new File(resourcePath);
        if (file == null) {
            Reader.serverFormattedResponseToClient("404", "File Not Found",
                    "the file you requested - " + decodedUri
                            + " does not exist on server" + "<hr>",
                    charStreamOutput, outputStream, "close");
            logger.error("the requested file doesnot exists on the server.Regret for inconvenience");
            return;
        }

        if (file.isDirectory()) {
            //ToDo: Need to return proper formatted response
            processDirectories(file, decodedUri);
            return;
        }

        Reader.clientResponseWithBody("200", "OK", file, charStreamOutput,
                outputStream, connectionStatus);
    }

    /**
     * this will handle the get requests for directory
     *
     * @param file       File object
     * @param requestUri request uri
     * @throws FileNotFoundException exception when file is not found
     */
    public static void processDirectories(File file, String requestUri)
            throws FileNotFoundException {
        File[] fileArray = file.listFiles();
        StringBuffer htmlLinks = new StringBuffer(100);
        for (int i = 0; i < fileArray.length; i++) {
            htmlLinks.append("<a href=\"" + requestUri
                    + WebServerConstants.URISeparator + fileArray[i].getName()
                    + "\">" + fileArray[i].getName() + "</a></BR>");
        }

        Reader.serverFormattedResponseToClient(
                "200",
                "OK",
                "The location you requested is a folder. Please follow links below to browse through the files .. <hr>"
                        + htmlLinks.toString(), charStreamOutput, outputStream,
                connectionStatus);
        return;
    }
}
