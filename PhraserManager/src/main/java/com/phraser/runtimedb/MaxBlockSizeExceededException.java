package com.phraser.runtimedb;

public class MaxBlockSizeExceededException extends RuntimeException {
    public MaxBlockSizeExceededException(int size, int maxSize) {
        super("Maximum block size exceeded: [" + size + "] > [" + maxSize + "]");
    }

    public MaxBlockSizeExceededException(String message) {
        super(message);
    }

    public MaxBlockSizeExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxBlockSizeExceededException(Throwable cause) {
        super(cause);
    }
}
