package com.adobe.web.readers;

import com.adobe.web.server.MalformedRequestException;
import com.adobe.web.utils.MimeTypeHandler;
import com.adobe.web.utils.ResponseCodeParams;
import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * It handles the request processing and response creation.
 */
public class Reader {
    private static Logger logger = Logger.getLogger(Reader.class.getName());

    /**
     * It reads the request line from the client which is
     * terminated by CRLF
     * @param inputStream Input stream from client socket connection.
     * @return Request line from the input stream.
     * @throws MalformedRequestException Throws if request is malformed.
     */
    public static String requestReader(InputStream inputStream)
            throws MalformedRequestException {
        StringBuffer requestLine = new StringBuffer(50);
        int character = 0;
        try {
            while ((character = inputStream.read()) > -1) {
                if ((char) character != '\r') {
                    requestLine.append((char) character);
                } else {
                    character = inputStream.read();
                    if ((char) character != '\n') {
                        logger.error("the request line is not properly formed");
                    }

                    return requestLine.toString();
                }
            }
        } catch (IOException ioException) {
            logger.error("The input stream cannot be read properly");
        }

        throw new MalformedRequestException("the request line is malformed");
    }

    /**
     *      *
     * @param statusCode            - the response status code
     * @param responseMessage       - the response message
     * @param fileObject            - the file to be returned
     * @param charStreamBufferedOut - streams connected to client
     * @param byteStreamBufferedOut - stream connected to client
     */
    /**
     * This method will return response to client with the File
     * @param statusCode Status code of response
     * @param responseMessage response message
     * @param fileObject file object
     * @param charStreamBufferedOut character stream writer
     * @param byteStreamBufferedOut output stream
     * @param Connection Connection details
     * @throws IOException Exception in case of input/output failures
     */
    public static void clientResponseWithBody(String statusCode,
                                              String responseMessage, File fileObject,
                                              BufferedWriter charStreamBufferedOut,
                                              BufferedOutputStream byteStreamBufferedOut, String Connection) throws IOException {
        StringBuffer outputMessage = new StringBuffer(100);
        outputMessage.append("HTTP/1.1" + " " + statusCode + " "
                + responseMessage);
        outputMessage.append(WebServerConstants.CRLF);
        outputMessage.append("Date:" + getServerTime());
        outputMessage.append(WebServerConstants.CRLF);
        outputMessage.append("Connection: " + Connection);
        outputMessage.append(WebServerConstants.CRLF);
        BufferedInputStream fileInput = null;
        fileInput = processResourceAsStream(fileObject, charStreamBufferedOut,
                byteStreamBufferedOut);
        String contentType = null;
        if (fileObject.getName() != null) {
            contentType = MimeTypeHandler.getInstance().findMimeType(
                    fileObject.getName());
        }

        if (contentType != null) {
            outputMessage.append("Content-Type: " + contentType);
            outputMessage.append(WebServerConstants.CRLF);
        }

        if (fileObject != null) {
            outputMessage.append("Content-Length: " + fileObject.length());
        }

        outputMessage.append(WebServerConstants.CRLF);
        outputMessage.append(WebServerConstants.CRLF);
        try {
            charStreamBufferedOut.write(outputMessage.toString());
            charStreamBufferedOut.flush();
            int c;
            while ((c = fileInput.read()) > -1) {
                byteStreamBufferedOut.write(c);
            }

            byteStreamBufferedOut.flush();
        } catch (IOException ioException) {
            logger.error("error in reading and writing to stream" + ioException.getMessage());
        }
    }

    /**
     * This will handle the formatted response to the client.
     * @param statusCode Status code
     * @param responseMessage Response message
     * @param formattedMessageBody Formatted response message
     * @param charStreamBufferedOut Character Stream output buffer
     * @param byteStreamBufferedOut Byte stream output buffer
     * @param Connection Connection details
     * @throws FileNotFoundException Exception if file is not present
     */
    public static void serverFormattedResponseToClient(String statusCode,
                                                       String responseMessage, String formattedMessageBody,
                                                       BufferedWriter charStreamBufferedOut,
                                                       BufferedOutputStream byteStreamBufferedOut, String Connection)
            throws FileNotFoundException {
        StringBuffer outputMessage = new StringBuffer(300);
        outputMessage.append("HTTP/1.1 " + statusCode + " " + responseMessage);
        outputMessage.append(WebServerConstants.CRLF);
        outputMessage.append("Server: " + "JAVA HTTP 1.1 Server");
        outputMessage.append(WebServerConstants.CRLF);
        outputMessage.append("Date:" + getServerTime());
        outputMessage.append(WebServerConstants.CRLF);
        outputMessage.append("Connection: " + Connection);
        outputMessage.append(WebServerConstants.CRLF);
        String messageBody = createFormattedMessageBody(statusCode,
                responseMessage, formattedMessageBody);

        outputMessage.append("Content-Length: " + messageBody.length());
        outputMessage.append(WebServerConstants.CRLF);
        outputMessage.append(WebServerConstants.CRLF);
        outputMessage.append(messageBody);
        try {
            byteStreamBufferedOut.write(outputMessage.toString().getBytes());
            byteStreamBufferedOut.flush();
        } catch (IOException ioException) {
            logger.error("error in reading and writing to stream" + ioException.getMessage());
        }
    }

    /**
     * This will create the message body
     * @param statusCode Satus code for output
     * @param responseMessage Response body
     * @param messageBody Message body for response
     * @return Complete formatted response body
     */
    public static String createFormattedMessageBody(String statusCode,
                                                    String responseMessage, String messageBody) {
        int length = messageBody.length();
        String body = null;
        if (messageBody == null || length == 0) {
            body = "<hr>nothing is there for you</hr>";
        }

        body = "<HTML>" + "<HEAD>" + "<TITLE>" + responseMessage + "</TITLE>"
                + "</HEAD>" + "<BODY>" + "<H3><center><font size=10>" + statusCode + ". "
                + responseMessage + "</font></center></H3>" + "</BR>" + "<p><center><font size=6>" + messageBody
                + "</font></center></p>" + "</BODY>" + "</HTML>";
        return body;
    }

    public static String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    /**
     * This will convert the file as stream
     * @param file File object
     * @param charStreamBufferedOut Character ouput buffer
     * @param outputStream Output stream
     * @return Stream of input file
     * @throws FileNotFoundException Exception if file is not present.
     */
    public static BufferedInputStream processResourceAsStream(File file,
                                                              BufferedWriter charStreamBufferedOut,
                                                              BufferedOutputStream outputStream) throws FileNotFoundException {
        BufferedInputStream fileResource = null;
        try {
            fileResource = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e1) {
            Reader.serverFormattedResponseToClient(ResponseCodeParams.FILE_NOT_FOUND, "Not Found",
                    "the file you requested - " + file.getName()
                            + " does not exist on server" + "<hr>",
                    charStreamBufferedOut, outputStream, "close");
            logger.info("file requested does not exist - " + file.getName());
        }

        return fileResource;
    }
}
