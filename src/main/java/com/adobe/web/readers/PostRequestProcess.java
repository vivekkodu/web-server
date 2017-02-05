package com.adobe.web.readers;

import com.adobe.web.server.MalformedRequestException;
import com.adobe.web.utils.ResponseCodeParams;
import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * It handles the post requests
 */
public class PostRequestProcess extends HttpRequestProcessor {

    private static File uploadDirectDefault = new File(WebServerConstants.HOSTPATH,
            WebServerConstants.UPLOAD_PATH);
    private static String uploadRelativePath = (File.separator + WebServerConstants.UPLOAD_PATH)
            .replace(File.separator, WebServerConstants.URISeparator).replaceAll(
                    WebServerConstants.URISeparator + "+", WebServerConstants.URISeparator);

    private static Logger logger = Logger.getLogger(PostRequestProcess.class
            .getName());
    private static String connectionStatus;

    /**
     * This will create a postRequestProcess object and initialize that with the
     * streams connected to the client
     *
     * @param bufferedByteInputStream  byte input stream
     * @param bufferedByeOutputStream  byte output stream
     * @param bufferedCharOutputStream character output stream
     */
    public PostRequestProcess(BufferedInputStream bufferedByteInputStream,
                              BufferedOutputStream bufferedByeOutputStream,
                              BufferedWriter bufferedCharOutputStream) {
        super(bufferedByteInputStream, bufferedByeOutputStream,
                bufferedCharOutputStream);
    }

    /**
     * This will handle post requests
     *
     * @param requestUri Request uri
     * @throws IOException               Exception in case of input/outpu failures
     * @throws MalformedRequestException Exception if uri is malformed
     */
    public void handlePostRequest(String requestUri) throws IOException,
            MalformedRequestException {
        Hashtable<String, String> headerList = this.getAllHeaders();
        connectionStatus = headerList.get("Connection");
        String decodedUri = decodeUri(requestUri);
        if (decodedUri == null) {
            //ToDo: Need to return proper formatted response
            logger.error("the requested uri cannot be decoded");
            return;
        }

        Hashtable<String, Object> propertyList = processHeaders(headerList);
        boolean isMultipart = (boolean) propertyList.containsKey("multipart");
        if (isMultipart) {
            String path = findLocationToUpload(decodedUri);
            if (path == null) {
                ReaderHelper.serverFormattedResponseToClient(
                        ResponseCodeParams.FILE_NOT_FOUND,
                        "Not Found",

                        "neither the location requested nor default location is available for upload of files"
                                + "<hr>", charStreamOutput, outputStream, "close");
                logger.info("upload location could not be created due to some security restrictions - ");
                return;
            }

            String pathToUpload = path;
            File file = new File(WebServerConstants.HOSTPATH,
                    pathToUpload.replace(WebServerConstants.URISeparator,
                            File.separator));
            String boundaryValue = (String) propertyList.get("boundary");
            ArrayList<String> uploadedFiles = new MultipartRequestProcess(
                    inputStream, outputStream, charStreamOutput)
                    .handleMultipartRequest("--" + boundaryValue, file);
            if (uploadedFiles != null) {
                logger.trace("file is uploaded on the server with the appropriate response");

                uploadResponse(uploadedFiles, pathToUpload);
            }

        } else {
            long contentLength = (Long) propertyList.get("Content-Length");
            byte[] messageBody = new byte[1024];
            while (contentLength > 0) {
                int len = (int) (contentLength > 1024 ? 1024 : contentLength);
                len = inputStream.read(messageBody, 0, len);
                for (int i = 0; i < len; i++)
                    logger.trace((char) messageBody[i]);
                contentLength -= len;

                ReaderHelper.serverFormattedResponseToClient(ResponseCodeParams.OK,
                        "OK", "request recieved and processed<hr>",
                        charStreamOutput, outputStream, connectionStatus);
            }
        }
    }

    /**
     * this will process the post request headers
     *
     * @param headerList Header list
     * @return hashtable having header list
     */
    public static Hashtable<String, Object> processHeaders(
            Hashtable<String, String> headerList) {
        Hashtable<String, Object> properties = new Hashtable<String, Object>(5);
        String boundaryValue = null;
        boolean isMultipart = false;
        String[] contentTypeArray = null;
        String contentType = headerList.get("Content-Type");
        if (contentType != null) {
            contentTypeArray = contentType.split(";");
            String[] mediaType = contentTypeArray[0].split("/");
            if (mediaType[0].trim().toLowerCase().equals("multipart")) {
                isMultipart = true;
                for (int i = 1; i < contentTypeArray.length; i++) {
                    String parameter = contentTypeArray[i].trim();
                    if (parameter.startsWith("boundary" + "=")) {
                        boundaryValue = parameter.split("=", 2)[1];
                    }

                    if (boundaryValue.charAt(0) == '"'
                            && boundaryValue.charAt(boundaryValue.length() - 1) == '"'
                            && boundaryValue.length() >= 2) {
                        boundaryValue = boundaryValue.substring(1,
                                boundaryValue.length() - 1);
                    }
                }

                if (boundaryValue == null) {
                    logger.error("boundary value is not properly formed");
                    try {
                        ReaderHelper.serverFormattedResponseToClient(
                                ResponseCodeParams.BAD_REQUEST,
                                "Bad request",
                                "boundary value not available in multipart request",
                                charStreamOutput, outputStream, "close");
                    } catch (FileNotFoundException e) {
                        logger.error("File cannot be found" + e.getMessage());
                    }

                    return null;
                }
            }
        }

        long length = -1;
        if (headerList.containsKey("Content-Length")) {
            try {
                length = Long.parseLong(headerList.get("Content-Length"));
            } catch (NumberFormatException e) {

            }
        }

        if (length < 0) {
            logger.error("Content-Length" + " should be properly set ");
            try {
                ReaderHelper.serverFormattedResponseToClient(
                        ResponseCodeParams.BAD_REQUEST, " Bad Request",
                        "Content-length" + " should be properly set" + "<hr>",
                        charStreamOutput, outputStream, "close");
            } catch (FileNotFoundException e) {
                logger.error("file cannot be found..it is a bad request"
                        + e.getMessage());

            }

            return null;
        }

        properties.put("Content-Length", length);
        properties.put("multipart", isMultipart);
        if (isMultipart) {
            properties.put("boundary", boundaryValue);
        }

        return properties;
    }

    /**
     * this will determine the upload path for the resources In case the upload
     * path doesnot exist,the resources are uploaded in the default directory
     *
     * @return the String containing the upload location for file
     */
    private static String findLocationToUpload(String requestURI) {
        File uploadDir = null;
        if (requestURI.length() > 0
                && !requestURI.equals(WebServerConstants.URISeparator)) {

            uploadDir = new File(WebServerConstants.HOSTPATH,
                    requestURI.replace(WebServerConstants.URISeparator,
                            File.separator));
            try {
                if (uploadDir.isDirectory()) {
                    return requestURI;
                }
                if (uploadDir.mkdirs()) {
                    return requestURI;
                }
            } catch (SecurityException e) {
                logger.error("error in trying to access upload dir provided with request uri due to some permission issues - "
                        + e.getMessage());
            }
        }

        try {
            if (uploadDirectDefault.isDirectory() || uploadDirectDefault.mkdirs()) {
                return uploadRelativePath;
            }
        } catch (SecurityException e) {
            logger.error("error in trying to access default upload dir - "
                    + e.getMessage());
        }

        return null;
    }

    private void uploadResponse(ArrayList<String> UploadedFiles, String location)
            throws IOException {
        StringBuffer HtmlLinks = new StringBuffer(100);
        Iterator<String> itr = UploadedFiles.iterator();
        while (itr.hasNext()) {
            String name = itr.next();
            HtmlLinks.append("<a href=" + "\""
                    + (location + WebServerConstants.URISeparator + name)
                    + "\"" + ">" + name + "</a>" + "</BR>");
        }

        ReaderHelper.serverFormattedResponseToClient(
                "201",
                "Created",
                "your data has been uploaded to the server. please follow the below links to check uploaded data<hr>"
                        + HtmlLinks.toString(), charStreamOutput, outputStream, "close");
    }
}
