package com.adobe.web.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * This class determines mime type of a file using its extension.
 * It reads mime type list from a file called mime.type and structures it into a HashMap
 * This is a singleton class
 */
public class MimeTypeHandler {

    private static MimeTypeHandler mimeTypeHandler = null;
    private HashMap<String, String> mimeTypes = null;


    /**
     * Return the instance of MimeTypes. This is a singleton class so new instances can't be created
     *
     * @return the MimeTypes Instance
     * @throws IOException if some error occurs read/write of mime.types
     */
    public static MimeTypeHandler getInstance() throws IOException {
        if (mimeTypeHandler == null) {
            synchronized (MimeTypeHandler.class) {
                if (mimeTypeHandler == null) {
                    mimeTypeHandler = new MimeTypeHandler();
                }
            }
        }

        return mimeTypeHandler;
    }

    private MimeTypeHandler() throws IOException {
        mimeTypes = new HashMap<String, String>(1000);
        BufferedReader reader = new BufferedReader(new FileReader("mime.types"));
        String next = null;
        String[] nextSplitted = null;
        while ((next = reader.readLine()) != null) {
            next = next.trim();
            if (next.charAt(0) != '#') {
                nextSplitted = next.split("\\s+");
                for (int i = 1; i < nextSplitted.length; i++) {
                    if (nextSplitted[i] != null && nextSplitted[i].length() != 0) {
                        mimeTypes.put(nextSplitted[i], nextSplitted[0]);
                    }
                }
            }
        }
    }

    /**
     * returns Mime type if found else null
     *
     * @param fileName name of file for which mime type has to be determined
     * @return mime type
     */
    public String findMimeType(String fileName) {
        String[] fileNameSplitted = fileName.split("\\.");
        String fileExtension = null;
        if (fileNameSplitted.length > 0) {
            fileExtension = fileNameSplitted[fileNameSplitted.length - 1];
        }

        if (fileExtension != null && fileExtension.length() != 0 && mimeTypes.containsKey(fileExtension)) {
            return mimeTypes.get(fileExtension);
        }

        return null;

    }

}
