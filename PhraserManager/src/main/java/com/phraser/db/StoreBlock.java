package com.phraser.db;

import org.immutables.value.Value;

@Value.Immutable
public interface StoreBlock {
    /** 16 bit */
    int blockId();

    /** 32 bit */
    int version();//32 bit

    /** 64 bit */
    long entropy();//64 bit
}
