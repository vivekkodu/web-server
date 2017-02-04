package com.adobe.web.readers;

import com.adobe.web.server.MalformedRequestException;
import com.adobe.web.utils.WebServerConstants;

import java.io.*;
import java.util.Hashtable;

public class DeleteRequestProcess extends HttpRequestProcessor {
    public DeleteRequestProcess(BufferedInputStream bufferedByteInputStream, BufferedOutputStream bufferedByeOutputStream, BufferedWriter bufferedCharOutputStream) {
        super(bufferedByteInputStream, bufferedByeOutputStream, bufferedCharOutputStream);
        System.out.println("in get handler constructor");
    }

    public static void handleDeleteRequest(String requestUri) throws IOException, MalformedRequestException {
        Hashtable<String, String> getHeaders = getAllHeaders();
        String decodedUri = decodeURI(requestUri);
        System.out.println("decoded uri" + decodedUri);
        if (decodedUri == null) {
            //ToDo: Need to return proper formatted response
            return;
        }

        if (decodedUri.equals(WebServerConstants.URISeparator)) {
            Reader.serverFormattedResponseToClient("404", "Resource not found", "the file you requested - " + decodedUri
                    + " does not exist on server" + "<hr>", charStreamOutput, outputStream, "close");
        }

        String resourcePath = getAbsoluteFilePath(decodedUri);
        System.out.println("resourcePath==" + resourcePath);
        File file = new File(resourcePath);
        System.out.println("file length is ==" + file.length());
        if (file == null) {
            System.out.println("file length" + file.length());
            Reader.serverFormattedResponseToClient("404", "File Not Found", "the file you requested - " + decodedUri
                    + " does not exist on server" + "<hr>", charStreamOutput, outputStream, "close");
            return;
        }

        if (file.isDirectory()) {
            //ToDo: Need to return proper formatted response
            return;
        }

        if (file.delete()) {
            //ToDo: Need to return proper formatted response
            System.out.println("file is deleted");
            return;
        }
    }
}
