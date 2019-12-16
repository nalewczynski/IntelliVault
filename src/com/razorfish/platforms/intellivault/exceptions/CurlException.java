package com.razorfish.platforms.intellivault.exceptions;

/**
 * An General Exception class for Errors that occur in IntelliVault.
 *
 * @author Sean Steimer
 */
public class CurlException extends Exception {

    public CurlException() {
        super();
    }

    public CurlException(String message) {
        super(message);
    }

    public CurlException(String message, Throwable cause) {
        super(message, cause);
    }

    public CurlException(Throwable cause) {
        super(cause);
    }
}
