package com.phraser.db;

import java.util.List;

public interface PhraseTemplatesBlock extends StoreBlock {
    interface WordTemplate {
        /** 16 bit */
        int wordTemplateId();
        /** 1 bit */
        int permissions();
        /** 1 bit */
        int icon();
        /** 32 bit */ //TODO: make 16 bit?
        int minLength();
        /** 32 bit */ //TODO: make 16 bit?
        int maxLength();
        String wordTemplateName();
        /** 16 bit array */
        List<Integer> symbolSetIds();
    }

    interface PhraseTemplate {
        /** 16 bit */
        int phraseTemplateId();
        String phraseTemplateName();
        List<WordTemplate> wordTemplates();
    }

    List<PhraseTemplate> phraseTemplates();
}
