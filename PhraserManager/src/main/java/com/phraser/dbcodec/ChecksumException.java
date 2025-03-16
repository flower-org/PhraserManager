package com.phraser.dbcodec;

public class ChecksumException extends RuntimeException {
    public ChecksumException() {
        super("Checksum validation failed.");
    }

    public ChecksumException(String message) {
        super(message);
    }

    public ChecksumException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChecksumException(Throwable cause) {
        super(cause);
    }
}
