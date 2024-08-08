package com.msg.plcaad.edc.ccp.exception;

public class CcpException extends Exception {
    public CcpException(String message) {
        super(message);
    }

    public CcpException(String message, Throwable cause) {
        super(message, cause);
    }
}
