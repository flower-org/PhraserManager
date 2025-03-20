package com.phraser.db;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface PhraseBlock extends StoreBlock {
    @Value.Immutable
    interface Word {
        /** 16 bit */
        int wordTemplateId();
        int wordTemplateOrdinal();
        String name();
        String word();
        byte permissions();
        Icon icon(); //standard icon code
    }

    @Value.Immutable
    interface PhraseHistory {
        /** 16 bit */
        int phraseTemplateId();
        List<Word> phrase();
    }

    /** 16 bit */
    int phraseTemplateId();
    /** 16 bit */
    int folderId();
    /** 1 byte */
    boolean isTombstone();
    String phraseName();

    List<PhraseHistory> history();
}
