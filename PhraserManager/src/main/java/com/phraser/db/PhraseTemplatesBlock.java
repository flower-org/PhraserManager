package com.phraser.db;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface PhraseTemplatesBlock extends StoreBlock {
    enum Icon {
        Key((byte)1),
        Login((byte)2),
        Asterisk((byte)3),
        Lock((byte)4),
        Aa((byte)5),
        Star((byte)6),

        Settings((byte)7),
        Folder((byte)8),
        ToParentFolder((byte)9),
        LookingGlass((byte)10),
        LTTriangle((byte)11),
        GTTriangle((byte)12),

        TextOut((byte)13),
        Ledger((byte)14),
        PlusMinus((byte)15),
        Stars((byte)16),
        Message((byte)17),
        Quote((byte)18),

        Question((byte)19),
        Plus((byte)20),
        Minus((byte)21),
        X((byte)22),
        Check((byte)23),
        Copy((byte)24);

        public final byte code;

        Icon(byte code) {
            this.code = code;
        }
    }

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
    }

    List<PhraseTemplate> phraseTemplates();
    List<WordTemplate> wordTemplates();
}
