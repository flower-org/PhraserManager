package com.phraser.db;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface SymbolSetsBlock extends StoreBlock {
    @Value.Immutable
    interface SymbolSet {
        /** 16 bit */
        int setId();
        String symbolSetName();
        char[] symbolSet();
    }

    List<SymbolSet> symbolSets();
}
