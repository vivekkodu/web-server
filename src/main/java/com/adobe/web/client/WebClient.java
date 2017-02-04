package com.adobe.web.client;

import com.adobe.web.readers.GetRequestProcess;
import com.adobe.web.readers.PostRequestProcess;
import com.adobe.web.readers.Reader;
import com.adobe.web.server.MalformedRequestException;
import com.adobe.web.utils.ResponseCodeParams;
import com.adobe.web.utils.WebServerConstants;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * This class will receive the request and triggers appropriate handler to
 * handle the request
 *
 * @author vivekkodu
 */

public class WebClient {

    private BufferedInputStream inputStream = null;
    private BufferedOutputStream outputstream = null;
    private BufferedWriter charStream = null;
    URI uriRequest = null;
    Socket clientSocket;
    String method;
    String version;
    String requestUri;
    public Hashtable<String, String> headers;
    Logger logger = Logger.getLogger(WebClient.class.getName());

    /**
     * This will serve the socket request which has been created.
     * @param clientSocket Client socket connection.
     */
    public WebClient(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void handleClient() {
        try {
            inputStream = new BufferedInputStream(clientSocket.getInputStream());
            outputstream = new BufferedOutputStream(
                    clientSocket.getOutputStream());
            charStream = new BufferedWriter(new OutputStreamWriter(
                    outputstream, WebServerConstants.HttpHeadersEncoding));

            String requestLine = Reader.requestReader(inputStream);
            logger.debug("requestLine is " + requestLine);
            StringTokenizer st = new StringTokenizer(requestLine, " ");
            int requestTokens = st.countTokens();

            // if request token is less than 3 ,it implies the request is not
            // properly formed.

            if (requestTokens < 3) {
                logger.error("the request line is not properly formed");
                throw new MalformedRequestException(
                        "the request line is malformed");
            }

            // Valid request will look like this GET /javadocs/resources/background.gif HTTP/1.1
            method = st.nextToken(); // this will contain the request method
            requestUri = st.nextToken(); // this will contain the request uri
            version = st.nextToken(); // this will contain the verison

            try {
                uriRequest = new URI(requestUri);
            } catch (URISyntaxException uriSyntaxException) {
                logger.error("uri is not properly formed"
                        + uriSyntaxException.getMessage());
                throw new MalformedRequestException(
                        "uri is not properly formed");
            }

            String requestPath = uriRequest.getPath();

            if (method.equalsIgnoreCase("GET")) {
                new GetRequestProcess(inputStream, outputstream, charStream)
                        .handleGetRequest(requestPath);
            } else if (method.equalsIgnoreCase("POST")) {
                new PostRequestProcess(inputStream, outputstream, charStream)
                        .handlePostRequest(requestPath);
            } else {
                Reader.serverFormattedResponseToClient(
                        ResponseCodeParams.METHOD_NOT_ALLOWED,
                        "Method not allowed",
                        "the  HTTP method you requested is not supported by the server",
                        charStream, outputstream, "close");
            }
        } catch (IOException ioException) {
            logger.error("read of input stream cannot be done properly"
                    + ioException.getMessage());

        } catch (MalformedRequestException malformedRequestException) {
            logger.error("uri is not properly formed"
                    + malformedRequestException.getMessage());
            try {
                Reader.serverFormattedResponseToClient(ResponseCodeParams.BAD_REQUEST,
                        "Bad request", "the request uri is not applicable", charStream, outputstream, "close");
            } catch (FileNotFoundException e) {
                logger.error("the requested uri is not applicable" + e.getMessage());
            }
        } finally {
            try {
                if (charStream != null)
                    charStream.close();
                charStream = null;
            } catch (IOException e) {
                logger.error("error in read/write of connection" + e.getMessage());
            }
            try {
                if (outputstream != null)
                    outputstream.close();
                outputstream = null;
            } catch (IOException e) {
                logger.error("error in read/write of connection" + e.getMessage());
            }
            try {
                if (inputStream != null)
                    inputStream.close();
                inputStream = null;
            } catch (IOException e) {
                logger.error("error in read/write of connection" + e.getMessage());
            }
            try {
                if (clientSocket != null)
                    clientSocket.close();
                clientSocket = null;
            } catch (IOException e) {
                logger.error("error in read/write of connection" + e.getMessage());
            }
        }

    }
}
