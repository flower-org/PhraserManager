package com.phraser.db;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface PhraseTemplatesBlock extends StoreBlock {
    @Value.Immutable
    interface WordTemplateRef {
        /** 16 bit */
        int wordTemplateId();
        /** 16 bit */
        int wordTemplateOrdinal();

        static WordTemplateRef of(int wordTemplateId, int wordTemplateOrdinal) {
            return ImmutableWordTemplateRef.builder()
                    .wordTemplateId(wordTemplateId)
                    .wordTemplateOrdinal(wordTemplateOrdinal)
                    .build();
        }
    }

    @Value.Immutable
    interface WordTemplate {
        /** 16 bit */
        int wordTemplateId();
        /** 8 bit (1 byte) */
        byte permissions();
        /** 8 bit (1 byte) */
        Icon icon();
        /** 16 bit */
        int minLength();
        /** 16 bit */
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
        List<WordTemplateRef> wordTemplateRefs();

        default int getId() { return phraseTemplateId(); }
        default String getName() {
            return phraseTemplateName();
        }

        static PhraseTemplate of(int phraseTemplateId, String phraseTemplateName, List<WordTemplateRef> wordTemplateRefs) {
            return ImmutablePhraseTemplate.builder()
                .phraseTemplateId(phraseTemplateId)
                .phraseTemplateName(phraseTemplateName)
                .wordTemplateRefs(wordTemplateRefs)
                .build();
        }
    }

    List<PhraseTemplate> phraseTemplates();
    List<WordTemplate> wordTemplates();
}
