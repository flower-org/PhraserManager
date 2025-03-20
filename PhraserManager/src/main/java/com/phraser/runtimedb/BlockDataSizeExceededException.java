package com.phraser.runtimedb;

import javax.annotation.Nullable;

public class BlockDataSizeExceededException extends RuntimeException {
    public final @Nullable Integer dataSize;
    public final @Nullable Integer maxSize;

    public BlockDataSizeExceededException(int dataSize, int maxSize) {
        super("Block data size exceeded: [" + dataSize + "] > [" + maxSize + "]");
        this.dataSize = dataSize;
        this.maxSize = maxSize;
    }

    public BlockDataSizeExceededException(String message) {
        super(message);
        this.dataSize = null;
        this.maxSize = null;
    }

    public BlockDataSizeExceededException(String message, Throwable cause) {
        super(message, cause);
        this.dataSize = null;
        this.maxSize = null;
    }

    public BlockDataSizeExceededException(Throwable cause) {
        super(cause);
        this.dataSize = null;
        this.maxSize = null;
    }
}
