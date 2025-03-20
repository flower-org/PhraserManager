package com.phraser.dbcodec;

import com.phraser.db.Icon;
import com.phraser.db.ImmutableFolder;
import com.phraser.db.ImmutableFoldersBlock;
import com.phraser.db.ImmutableKeyBlock;
import com.phraser.db.ImmutablePhraseBlock;
import com.phraser.db.ImmutablePhraseHistory;
import com.phraser.db.ImmutablePhraseTemplate;
import com.phraser.db.ImmutablePhraseTemplatesBlock;
import com.phraser.db.ImmutableSymbolSet;
import com.phraser.db.ImmutableSymbolSetsBlock;
import com.phraser.db.ImmutableWord;
import com.phraser.db.ImmutableWordTemplate;
import com.phraser.db.ImmutableWordTemplateRef;
import com.phraser.schema.phraser.Folder;
import com.phraser.schema.phraser.FoldersBlock;
import com.phraser.schema.phraser.KeyBlock;
import com.phraser.schema.phraser.PhraseBlock;
import com.phraser.schema.phraser.PhraseHistory;
import com.phraser.schema.phraser.PhraseTemplate;
import com.phraser.schema.phraser.PhraseTemplatesBlock;
import com.phraser.schema.phraser.SymbolSet;
import com.phraser.schema.phraser.SymbolSetsBlock;
import com.phraser.schema.phraser.Word;
import com.phraser.schema.phraser.WordTemplate;
import com.phraser.schema.phraser.WordTemplateRef;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class FlatBufBlockDecoder {
    public static String getString(int length, Function<Integer, Byte> getChar) {
        return new String(getBytes(length, getChar), StandardCharsets.UTF_8);
    }

    public static byte[] getBytes(int length, Function<Integer, Byte> getChar) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = getChar.apply(i);
        }
        return bytes;
    }

    public static com.phraser.db.KeyBlock fromFlatBufKeyBlock(byte[] data) {
        // Convert byte array to ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        // Get the root object from the ByteBuffer
        KeyBlock keyBlock = KeyBlock.getRootAsKeyBlock(byteBuffer);

        byte[] key = getBytes(keyBlock.keyLength(), keyBlock::key);
        byte[] iv = getBytes(keyBlock.ivLength(), keyBlock::iv);
        String dbName = getString(keyBlock.dbNameLength(), keyBlock::dbName);

        // Create a new KeyBlock object to return
        return ImmutableKeyBlock.builder()
                .blockId(keyBlock.block().blockId())
                .version(keyBlock.block().version())
                .entropy(keyBlock.block().entropy())
                .blockCount(keyBlock.blockCount())
                .dbName(dbName)
                .key(key)
                .iv(iv)
                .build();
    }

    public static com.phraser.db.SymbolSetsBlock fromFlatBufSymbolSetsBlock(byte[] data) {
        // Convert byte array to ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        // Get the root object from the ByteBuffer
        SymbolSetsBlock symbolSetsBlock = SymbolSetsBlock.getRootAsSymbolSetsBlock(byteBuffer);

        // Retrieve symbol sets
        int symbolSetCount = symbolSetsBlock.symbolSetsLength();
        List<com.phraser.db.SymbolSetsBlock.SymbolSet> symbolSets = new ArrayList<>();
        for (int i = 0; i < symbolSetCount; i++) {
            SymbolSet symbolSet = symbolSetsBlock.symbolSets(i);

            com.phraser.db.SymbolSetsBlock.SymbolSet decodedSymbolSet =
                    ImmutableSymbolSet.builder()
                            .symbolSetId(symbolSet.setId())
                            .symbolSetName(checkNotNull(symbolSet.symbolSetName()))
                            .symbolSet(checkNotNull(symbolSet.symbolSet()).toCharArray())
                            .build();
            symbolSets.add(decodedSymbolSet);
        }

        // Create a new SymbolSetsBlock object to return
        return ImmutableSymbolSetsBlock.builder()
                .blockId(symbolSetsBlock.block().blockId())
                .version(symbolSetsBlock.block().version())
                .entropy(symbolSetsBlock.block().entropy())
                .symbolSets(symbolSets)
                .build();
    }

    public static com.phraser.db.FoldersBlock fromFlatBufFoldersBlock(byte[] data) {
        // Convert byte array to ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        // Get the root object from the ByteBuffer
        FoldersBlock foldersBlock = FoldersBlock.getRootAsFoldersBlock(byteBuffer);

        // Retrieve folders
        int folderCount = foldersBlock.foldersLength();
        List<com.phraser.db.FoldersBlock.Folder> folders = new ArrayList<>();
        for (int i = 0; i < folderCount; i++) {
            Folder folder = foldersBlock.folders(i);
            com.phraser.db.FoldersBlock.Folder decodedFolder = ImmutableFolder.builder()
                    .folderId(folder.folderId())
                    .parentFolderId(folder.parentFolderId())
                    .folderName(checkNotNull(folder.folderName()))
                    .build();
            folders.add(decodedFolder);
        }

        // Create a new FoldersBlock object to return
        return ImmutableFoldersBlock.builder()
                .blockId(foldersBlock.block().blockId())
                .version(foldersBlock.block().version())
                .entropy(foldersBlock.block().entropy())
                .folders(folders)
                .build();
    }

    public static com.phraser.db.PhraseTemplatesBlock fromFlatBufPhraseTemplatesBlock(byte[] data) {
        // Convert byte array to ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        // Get the root object from the ByteBuffer
        PhraseTemplatesBlock phraseTemplatesBlock = PhraseTemplatesBlock.getRootAsPhraseTemplatesBlock(byteBuffer);

        // Retrieve word templates
        int wordTemplateCount = phraseTemplatesBlock.wordTemplatesLength();
        List<com.phraser.db.PhraseTemplatesBlock.WordTemplate> wordTemplates = new ArrayList<>();
        for (int i = 0; i < wordTemplateCount; i++) {
            WordTemplate wordTemplate = phraseTemplatesBlock.wordTemplates(i);

            // Retrieve symbol set IDs
            int symbolSetIdCount = wordTemplate.symbolSetIdsLength();
            List<Integer> symbolSetIds = new ArrayList<>();
            for (int j = 0; j < symbolSetIdCount; j++) {
                symbolSetIds.add(wordTemplate.symbolSetIds(j));
            }

            com.phraser.db.PhraseTemplatesBlock.WordTemplate decodedWordTemplate = ImmutableWordTemplate.builder()
                    .wordTemplateId(wordTemplate.wordTemplateId())
                    .permissions(wordTemplate.permissions())
                    .icon(Icon.fromCode(wordTemplate.icon()))
                    .minLength(wordTemplate.minLength())
                    .maxLength(wordTemplate.maxLength())
                    .wordTemplateName(checkNotNull(wordTemplate.wordTemplateName()))
                    .symbolSetIds(symbolSetIds)
                    .build();

            wordTemplates.add(decodedWordTemplate);
        }

        // Retrieve phrase templates
        int phraseTemplateCount = phraseTemplatesBlock.phraseTemplatesLength();
        List<com.phraser.db.PhraseTemplatesBlock.PhraseTemplate> phraseTemplates = new ArrayList<>();
        for (int i = 0; i < phraseTemplateCount; i++) {
            PhraseTemplate phraseTemplate = phraseTemplatesBlock.phraseTemplates(i);

            // Retrieve word template IDs
            int wordTemplateRefsCount = phraseTemplate.wordTemplateRefsLength();
            List<com.phraser.db.PhraseTemplatesBlock.WordTemplateRef> wordTemplateRefs = new ArrayList<>();
            for (int j = 0; j < wordTemplateRefsCount; j++) {
                WordTemplateRef wordTemplateRefSrc = phraseTemplate.wordTemplateRefs(j);
                com.phraser.db.PhraseTemplatesBlock.WordTemplateRef wordTemplateRef = ImmutableWordTemplateRef.builder()
                        .wordTemplateId(wordTemplateRefSrc.wordTemplateId())
                        .wordTemplateOrdinal(wordTemplateRefSrc.wordTemplateOrdinal())
                        .build();
                wordTemplateRefs.add(wordTemplateRef);
            }

            com.phraser.db.PhraseTemplatesBlock.PhraseTemplate decodedPhraseTemplate = ImmutablePhraseTemplate.builder()
                    .phraseTemplateId(phraseTemplate.phraseTemplateId())
                    .phraseTemplateName(checkNotNull(phraseTemplate.phraseTemplateName()))
                    .wordTemplateRefs(wordTemplateRefs)
            .build();

            phraseTemplates.add(decodedPhraseTemplate);
        }

        // Create a new PhraseTemplatesBlock object to return
        return ImmutablePhraseTemplatesBlock.builder()
                .blockId(phraseTemplatesBlock.block().blockId())
                .version(phraseTemplatesBlock.block().version())
                .entropy(phraseTemplatesBlock.block().entropy())
                .wordTemplates(wordTemplates)
                .phraseTemplates(phraseTemplates)
                .build();
    }

    public static com.phraser.db.PhraseBlock fromFlatBufPhraseBlock(byte[] data) {
        // Get the root object from the FlatBuffer data
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        PhraseBlock phraseBlock = PhraseBlock.getRootAsPhraseBlock(byteBuffer);

        // Reconstruct the PhraseHistory list
        List<com.phraser.db.PhraseBlock.PhraseHistory> phraseHistories = new ArrayList<>();
        int historyLength = phraseBlock.historyLength();
        for (int i = 0; i < historyLength; i++) {
            PhraseHistory phraseHistory = phraseBlock.history(i);
            int phraseTemplateId = phraseHistory.phraseTemplateId();

            // Reconstruct the words in the phrase
            List<com.phraser.db.PhraseBlock.Word> words = new ArrayList<>();
            int phraseLength = phraseHistory.phraseLength();
            for (int j = 0; j < phraseLength; j++) {
                Word word = phraseHistory.phrase(j);
                com.phraser.db.PhraseBlock.Word reconstructedWord = ImmutableWord.builder()
                        .wordTemplateId(word.wordTemplateId())
                        .wordTemplateOrdinal(word.wordTemplateOrdinal())
                        .name(checkNotNull(word.name()))
                        .word(checkNotNull(word.word()))
                        .permissions(word.permissions())
                        .icon(Icon.fromCode(word.icon()))
                        .build();
                words.add(reconstructedWord);
            }

            com.phraser.db.PhraseBlock.PhraseHistory reconstructedPhraseHistory = ImmutablePhraseHistory.builder()
                    .phraseTemplateId(phraseTemplateId)
                    .phrase(words)
                    .build();
            phraseHistories.add(reconstructedPhraseHistory);
        }

        // Create the PhraseBlock object
        return ImmutablePhraseBlock.builder()
                .blockId(phraseBlock.block().blockId())
                .version(phraseBlock.block().version())
                .entropy(phraseBlock.block().entropy())
                .phraseTemplateId(phraseBlock.phraseTemplateId())
                .folderId(phraseBlock.folderId())
                .isTombstone(phraseBlock.isTombstone())
                .phraseName(checkNotNull(phraseBlock.phraseName()))
                .history(phraseHistories)
                .build();
    }
}