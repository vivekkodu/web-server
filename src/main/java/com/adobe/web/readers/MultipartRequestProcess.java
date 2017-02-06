package com.adobe.web.readers;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * this class will parse the multipart request using the boundary
 */
public class MultipartRequestProcess {

    protected BufferedInputStream inputStream;
    protected BufferedOutputStream outputStream;
    protected BufferedWriter charStreamBufferedOut;
    protected String contentDisposition = "Content-Disposition";
    private static Logger logger = Logger
            .getLogger(MultipartRequestProcess.class.getName());

    /**
     * This will create a MultipartRequestProcess object initialized with the
     * streams connected to the client
     *
     * @param byteStreamBufferedIn  byte input stream
     * @param byteStreamBufferedOut byte output stream
     * @param charStreamBufferedOut char output stream
     */
    public MultipartRequestProcess(BufferedInputStream byteStreamBufferedIn,
                                   BufferedOutputStream byteStreamBufferedOut,
                                   BufferedWriter charStreamBufferedOut) {
        inputStream = byteStreamBufferedIn;
        outputStream = byteStreamBufferedOut;
        this.charStreamBufferedOut = charStreamBufferedOut;
    }

    /**
     * This will return the preprocess pattern used in kmp search
     *
     * @param pattern pattern
     * @return an integer array denoting the preprocess pattern
     */
    private int[] preProcessPattern(String pattern) {
        int i = 0, j = -1;
        int ptrnLen = pattern.length();
        int[] preProcessPattern = new int[ptrnLen + 1];

        preProcessPattern[i] = j;
        while (i < ptrnLen) {
            while (j >= 0 && pattern.charAt(i) != pattern.charAt(j)) {
                j = preProcessPattern[j];
            }
            i++;
            j++;
            preProcessPattern[i] = j;
        }
        return preProcessPattern;
    }

    /**
     * this will search for the boundary in the message body
     */
    private static void searchSubString(InputStream inputStream,
                                        String boundaryValue, int[] preProcessPattern) {
        int j = 0;
        int c;
        // pattern and text lengths

        int boundaryLength = boundaryValue.length();

        // initialize new array and preprocess the pattern

        try {
            while ((c = inputStream.read()) > -1) {

                while (j >= 0 && (char) c != boundaryValue.charAt(j)) {
                    j = preProcessPattern[j];
                }

                j++;

                // a match is found
                if (j == boundaryValue.length()) {
                    return;
                }
            }
        } catch (IOException e) {
            logger.error("there is an error in reading the stream connected to the client"
                    + e.getMessage());

        }

    }

    /**
     * This will dump the resource content in the file along with the boundary.
     * After that boundary value is deleted from the file and the file is
     * written to the output stream
     */
    private static void searchSubstringFile(InputStream inputStream,
                                            OutputStream out, String boundaryValue, int[] preProcessPattern)
            throws IOException {

        int j = 0;
        int c;
        // pattern and text lengths

        int boundaryLength = boundaryValue.length();

        // initialize new array and preprocess the pattern

        try {
            while ((c = inputStream.read()) > -1) {

                while (j >= 0 && (char) c != boundaryValue.charAt(j)) {
                    j = preProcessPattern[j];
                }

                j++;
                out.write(c);

                // a match is found
                if (j == boundaryValue.length()) {
                    out.flush();

                    return;
                }
            }
        } catch (IOException e) {
            logger.error("error in reading and writing the stream connected to the client"
                    + e.getMessage());
        }

    }

    /**
     * this will split the headers acc to the delim1 and delim2 and store the
     * key value pair in the hash table
     */
    private Hashtable<String, String> convertHeaderToHashTable(String header,
                                                               String regex1, String regex2) {
        Hashtable<String, String> headerList = new Hashtable<String, String>(20);
        if (header != null && regex1 != null && regex2 != null
                && header.length() > 0 && regex1.length() > 0
                && regex2.length() > 0) {

            String firstSplit[] = header.split(regex1);
            for (int i = 0; i < firstSplit.length; i++) {

                String secondSplit[] = firstSplit[i].trim().split(regex2, 2);
                if (secondSplit.length == 2 && secondSplit[0].length() > 0
                        && secondSplit[1].length() > 0) {

                    headerList.put(secondSplit[0], secondSplit[1]);
                }
            }

        }

        return headerList;

    }

    /**
     * This will process the multipart headers to determine whether the file name exists or not
     */
    private String processHeaders(Hashtable<String, String> headerList) {
        String fileName = null;
        Hashtable<String, String> headerTable;
        if (headerList.containsKey(contentDisposition)) {

            String value = headerList.get(contentDisposition);
            if (value != null) {
                headerTable = convertHeaderToHashTable(value, ";", "=");
                if (headerTable.containsKey("filename")) {
                    fileName = headerTable.get("filename");
                    if (fileName.length() >= 2) {
                        fileName = fileName.substring(1, fileName.length() - 1);
                    } else if (fileName.length() == 1) {
                        fileName = null;
                        logger.error("The file name doesnot exists");
                    } else {
                        fileName = null;
                    }
                }
            }
            if (fileName == null) {

                logger.error("file name is wrong and hence returning null");
                return null;
            }
            File temporary = new File(fileName);

            return temporary.getName();

        }

        return null;
    }

    /**
     * this is the method to handle the incoming multipart request
     *
     * @param boundaryValue-boundary value in the message body
     * @param location-file          name
     * @return-the list of files that are uploaded on the server
     * @throws IOException Exception in case of input/output failures
     */
    public ArrayList<String> handleMultipartRequest(String boundaryValue, File location)
            throws IOException {
        int[] preProcessPattern = preProcessPattern(boundaryValue);
        searchSubString(inputStream, boundaryValue, preProcessPattern);
        ArrayList<String> filesUploaded = new ArrayList<String>(5);

        while (!isMessageBodyCompleted(inputStream)) {

            String messageHeader = HeaderHandler.readHeader(inputStream);
            Hashtable<String, String> headerTable = HeaderHandler
                    .getClientHeaders(messageHeader);


            String fileName = processHeaders(headerTable);


            if (fileName == null) {
                logger.trace("the list of files uploaded is returned");
                return filesUploaded;
            }

            // ignore this file
            if (fileName.length() == 0) {
                searchSubString(inputStream, boundaryValue, preProcessPattern);
                continue;
            }

            if (!upLoadFile(fileName, location, boundaryValue,
                    preProcessPattern)) {
                continue;
            }

            filesUploaded.add(fileName);
        }

        return filesUploaded;
    }

    /**
     * This will determine whether the message body in the multipart request is completed
     *
     * @return boolean value denoting whether message body ends
     */
    private boolean isMessageBodyCompleted(BufferedInputStream inputStream) {
        int firstChar = 0;
        try {
            firstChar = inputStream.read();
        } catch (IOException e) {
            logger.error("there is an error in reading the stream" + e.getMessage());

        }
        int secondChar = 0;
        try {
            secondChar = inputStream.read();
        } catch (IOException e) {

            logger.error("there is an error in reading the stream" + e.getMessage());
        }
        if ((char) firstChar == '-' && (char) secondChar == '-') {

            return true;
        }
        return false;

    }

    /**
     * this will upload the files on the server
     *
     * @return boolean value whether file is uploaded or not
     */
    private boolean upLoadFile(String fileName, File location,
                               String boundaryValue, int[] preProcessPattern) throws IOException {
        File fileToUpload = new File(location.getAbsolutePath(), fileName);
        BufferedOutputStream uploadedFile = null;
        try {
            try {
                uploadedFile = new BufferedOutputStream(new FileOutputStream(
                        fileToUpload));
            } catch (FileNotFoundException e) {
                logger.error("file named " + fileName + ",can't be created");
                searchSubString(inputStream, boundaryValue, preProcessPattern);
                return false; // indicates continue
            }
            searchSubstringFile(inputStream, uploadedFile, boundaryValue,
                    preProcessPattern);

        } finally {
            if (uploadedFile != null) {
                try {
                    uploadedFile.flush();
                } catch (IOException e) {
                    logger.error("file named " + fileName
                            + ",written but could not flush");
                }
                try {
                    uploadedFile.close();
                } catch (IOException e) {
                    logger.error("file named " + fileName
                            + ",could not be closed");
                }
            }
        }

        try {
            ReaderHelper.DeleteBoundaryFromFile(fileToUpload, boundaryValue);
        } catch (IOException e) {
            logger.error("file named "
                    + fileName
                    +
                    ",uploaded but is appended a multipart boundary .. could not delete");
            return false; // indicates continue
        }

        logger.info("file created successfully at location - "
                + fileToUpload.getAbsolutePath());
        // indicates go forward
        return true;
    }
}