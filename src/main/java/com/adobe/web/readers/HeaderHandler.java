package com.adobe.web.readers;

import com.adobe.web.utils.WebServerConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * This class processes the request header.
 */
public class HeaderHandler {

    /**
     * This class will read each line and determine if the message body is
     * reached terminated by two CRLF's otherwise it will append each line to the
     * String buffer
     *
     * @param clientInput input stream
     * @return - a string containing all the header fields
     * @throws IOException exception in input or output
     */
    public static String readHeader(InputStream clientInput)
            throws IOException {

        StringBuffer clientHeaders = new StringBuffer(100);
        int character;
        int final_state = 4;
        int intermediate_state = 0;
        while (intermediate_state != final_state) {
            if ((character = clientInput.read()) > -1) {
                switch ((char) character) {
                    case '\r':
                        if (intermediate_state == 0) {
                            intermediate_state = 1;
                        } else if (intermediate_state == 2) {
                            intermediate_state = 3;
                        }

                        break;
                    case '\n':
                        if (intermediate_state == 1) {
                            intermediate_state = 2;
                        } else if (intermediate_state == 3) {
                            intermediate_state = 4;
                        }

                        break;
                    default:
                        intermediate_state = 0;
                }

                clientHeaders.append((char) character);
            }
        }

        if (intermediate_state != final_state) {

        }

        clientHeaders.deleteCharAt(clientHeaders.length() - 1);
        clientHeaders.deleteCharAt(clientHeaders.length() - 1);
        return clientHeaders.toString();
    }

    /**
     * This method will create a Hashtable with the key as header name and
     * value as header value
     *
     * @param clientHeaders string representation of client header
     * @return -a hashtable with header name and header value as the key value
     * pair
     */
    public static Hashtable<String, String> getClientHeaders(
            String clientHeaders) {

        if (clientHeaders == null) {
            return null;
        }

        Hashtable<String, String> headerTable = new Hashtable<String, String>(
                50);
        // splitting the string with the regex as "\r\n"

        String[] headerList = clientHeaders.split(WebServerConstants.CRLF);
        int length = headerList.length;
        for (int i = 0; i < length && headerList[i] != null
                && headerList[i].length() > 0; i++) {
            String property[] = getHeaderProperties(headerList[i]);
            if (property != null) {
                // if the header name is already present then the second header
                // value is appended to the name
                if (headerTable.containsKey(property[0])) {
                    String value = headerTable.get(property[0]) + ","
                            + property[1];
                    headerTable.put(property[0], value);
                } else {
                    headerTable.put(property[0], property[1]);
                }
            }
        }

        return headerTable;
    }

    /**
     * This is used for splitting the multipart header
     *
     * @param header header
     * @return array of header parts
     */
    public static String[] getHeaderProperties(String header) {

        header = header.replaceAll("\\s+", " ");
        String[] strArr = header.split(":", 2);
        if (strArr[0] != null && strArr[1] != null) {
            strArr[0] = strArr[0].trim();
            strArr[1] = strArr[1].trim();
            if (strArr[0].length() != 0 && strArr[1].length() != 0)
                return strArr;
        }

        return null;
    }
}
