package com.adobe.web.server;

/**
 * This class is used for denoting malformed request exception
 */
public class MalformedRequestException extends Exception {
    private static final long serialVersionUID = 1L;

    public MalformedRequestException(String exceptionMessage) {
        super(exceptionMessage);
    }
}