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
        default String getSymbolSet() { return new String(symbolSet()); }

        static SymbolSet of(int symbolSetId, String symbolSetName, char[] symbolSet) {
            return ImmutableSymbolSet.builder()
                .symbolSetId(symbolSetId)
                .symbolSetName(symbolSetName)
                .symbolSet(symbolSet)
                .build();
        }
    }

    List<SymbolSet> symbolSets();
}
