package com.phraser.db;

import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;

@Value.Immutable
public interface SymbolSetsBlock extends StoreBlock {
    @Value.Immutable
    interface SymbolSet {
        /** 16 bit */
        int symbolSetId();
        String symbolSetName();
        char[] symbolSet();

        default int getId() { return symbolSetId(); }
        default String getName() { return symbolSetName(); }
        default String getSymbolSet() { return Arrays.toString(symbolSet()); }
    }

    List<SymbolSet> symbolSets();
}
