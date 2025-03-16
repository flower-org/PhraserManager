package com.phraser.db;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface PhraseTemplatesBlock extends StoreBlock {
    @Value.Immutable
    interface WordTemplate {
        /** 16 bit */
        int wordTemplateId();
        /** 1 bit */
        byte permissions();
        /** 1 bit */
        Icon icon();
        /** 32 bit */ //TODO: make 16 bit?
        int minLength();
        /** 32 bit */ //TODO: make 16 bit?
        int maxLength();
        String wordTemplateName();
        /** 16 bit array */
        List<Integer> symbolSetIds();

        default int getId() { return wordTemplateId(); }
        default String getName() {
            return wordTemplateName();
        }

        static WordTemplate of(int wordTemplateId,
                               byte permissions,
                               Icon icon,
                               int minLength,
                               int maxLength,
                               String wordTemplateName,
                               List<Integer> symbolSetIds) {
            return ImmutableWordTemplate.builder()
                .wordTemplateId(wordTemplateId)
                .permissions(permissions)
                .icon(icon)
                .minLength(minLength)
                .maxLength(maxLength)
                .wordTemplateName(wordTemplateName)
                .symbolSetIds(symbolSetIds)
                .build();
        }
    }

    @Value.Immutable
    interface PhraseTemplate {
        /** 16 bit */
        int phraseTemplateId();
        String phraseTemplateName();
        List<Integer> wordTemplateIds();

        default int getId() { return phraseTemplateId(); }
        default String getName() {
            return phraseTemplateName();
        }

        static PhraseTemplate of(int phraseTemplateId, String phraseTemplateName, List<Integer> wordTemplateIds) {
            return ImmutablePhraseTemplate.builder()
                .phraseTemplateId(phraseTemplateId)
                .phraseTemplateName(phraseTemplateName)
                .wordTemplateIds(wordTemplateIds)
                .build();
        }
    }

    List<PhraseTemplate> phraseTemplates();
    List<WordTemplate> wordTemplates();
}
