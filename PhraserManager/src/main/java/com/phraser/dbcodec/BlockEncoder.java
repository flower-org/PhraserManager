package com.phraser.dbcodec;

import com.google.flatbuffers.FlatBufferBuilder;
import com.phraser.db.Block;
import com.phraser.db.BlockType;
import com.phraser.db.FoldersBlock;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.StoreBlock;
import com.phraser.db.SymbolSetsBlock;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class BlockEncoder {
    public static byte[] toFlatBufBlock(Block block) {
        BlockType blockType = block.blockType();
        switch (blockType) {
            case KEY_BLOCK:
                return toKeyBlock(checkNotNull(block.keyBlock()));
            case SYMBOL_SETS_BLOCK:
                return toFlatBufSymbolSetsBlock(checkNotNull(block.symbolSetsBlock()));
            case FOLDERS_BLOCK:
                return toFlatBufFoldersBlock(checkNotNull(block.foldersBlock()));
            case PHRASE_TEMPLATES_BLOCK:
                return toFlatBufPhraseTemplatesBlock(checkNotNull(block.phraseTemplatesBlock()));
            case PHRASE_BLOCK:
                return toFlatBufPhraseBlock(checkNotNull(block.phraseBlock()));
            default:
                throw new RuntimeException("Unsupported Block type: " + blockType);
        }
    }

    public static byte[] toFlatBufPhraseBlock(PhraseBlock phraseBlock) {
        StoreBlock storeBlock = phraseBlock;
        FlatBufferBuilder builder = new FlatBufferBuilder(12000);

        List<PhraseBlock.PhraseHistory> history = checkNotNull(phraseBlock).history();
        return new byte[42];

        //TODO: implement
/*        int[] wordTemplateOffsets = new int[wordTemplates.size()];
        for (int i = 0; i < wordTemplates.size(); i++) {
            PhraseTemplatesBlock.WordTemplate wordTemplate = wordTemplates.get(i);

            int wordTemplateId = wordTemplate.wordTemplateId();
            byte permissions = wordTemplate.permissions();
            byte icon = wordTemplate.icon().code;
            int minLength = wordTemplate.minLength();
            int maxLength = wordTemplate.maxLength();

            int wordTemplateNameOffset = builder.createString(wordTemplate.wordTemplateName());
            int[] symbolSetIds = toIdArray(wordTemplate.symbolSetIds());
            int symbolSetIdsOffset = com.phraser.schema.phraser.WordTemplate.createSymbolSetIdsVector(builder, symbolSetIds);

            int wordTemplateOffset = com.phraser.schema.phraser.WordTemplate.createWordTemplate(builder,
                    wordTemplateId, permissions, icon, minLength, maxLength, wordTemplateNameOffset, symbolSetIdsOffset);

            wordTemplateOffsets[i] = wordTemplateOffset;
        }

        int wordTemplatesOffset =
                com.phraser.schema.phraser.PhraseTemplatesBlock.createWordTemplatesVector(builder, wordTemplateOffsets);

        // -----------------------------------------------------------------------

        List<PhraseTemplatesBlock.PhraseTemplate> phraseTemplates =
                checkNotNull(phraseTemplatesBlock).phraseTemplates();

        int[] phraseTemplateOffsets = new int[phraseTemplates.size()];
        for (int i = 0; i < phraseTemplates.size(); i++) {
            PhraseTemplatesBlock.PhraseTemplate phraseTemplate = phraseTemplates.get(i);

            int phraseTemplateId = phraseTemplate.phraseTemplateId();
            int phraseTemplateNameOffset = builder.createString(phraseTemplate.phraseTemplateName());
            int[] wordTemplateIds = toIdArray(phraseTemplate.wordTemplateIds());

            int wordTemplateIdsOffset = com.phraser.schema.phraser.PhraseTemplate.createWordTemplateIdsVector(builder, wordTemplateIds);

            int phraseTemplateOffset = com.phraser.schema.phraser.PhraseTemplate.createPhraseTemplate(builder,
                    phraseTemplateId, phraseTemplateNameOffset, wordTemplateIdsOffset);

            phraseTemplateOffsets[i] = phraseTemplateOffset;
        }

        int phraseTemplatesOffset =
                com.phraser.schema.phraser.PhraseTemplatesBlock.createPhraseTemplatesVector(builder, phraseTemplateOffsets);

        // -----------------------------------------------------------------------

        com.phraser.schema.phraser.PhraseTemplatesBlock.startPhraseTemplatesBlock(builder);

        int storeBlockOffset = com.phraser.schema.phraser.StoreBlock.createStoreBlock(builder,
                storeBlock.blockId(), storeBlock.version(), storeBlock.entropy());

        com.phraser.schema.phraser.PhraseTemplatesBlock.addBlock(builder, storeBlockOffset);
        com.phraser.schema.phraser.PhraseTemplatesBlock.addPhraseTemplates(builder, phraseTemplatesOffset);
        com.phraser.schema.phraser.PhraseTemplatesBlock.addWordTemplates(builder, wordTemplatesOffset);
        int symbolSetsBlockOffset = com.phraser.schema.phraser.FoldersBlock.endFoldersBlock(builder);

        builder.finish(symbolSetsBlockOffset);

        return builder.sizedByteArray();*/
    }

    public static byte[] toKeyBlock(KeyBlock keyBlock) {
        StoreBlock storeBlock = keyBlock;
        FlatBufferBuilder builder = new FlatBufferBuilder(12000);

        com.phraser.schema.phraser.KeyBlock.startKeyBlock(builder);

        int storeBlockOffset = com.phraser.schema.phraser.StoreBlock.createStoreBlock(builder,
                storeBlock.blockId(), storeBlock.version(), storeBlock.entropy());
        int dbNameOffset = builder.createString(keyBlock.dbName());
        int keyOffset = builder.createByteVector(keyBlock.key());
        int ivOffset = builder.createByteVector(keyBlock.iv());

        com.phraser.schema.phraser.KeyBlock.addBlock(builder, storeBlockOffset);
        com.phraser.schema.phraser.KeyBlock.addBucketCount(builder, keyBlock.bucketCount());
        com.phraser.schema.phraser.KeyBlock.addDbName(builder, dbNameOffset);
        com.phraser.schema.phraser.KeyBlock.addKey(builder, keyOffset);
        com.phraser.schema.phraser.KeyBlock.addIv(builder, ivOffset);

        return builder.sizedByteArray();
    }

    public static byte[] toFlatBufPhraseTemplatesBlock(PhraseTemplatesBlock phraseTemplatesBlock) {
        StoreBlock storeBlock = phraseTemplatesBlock;
        FlatBufferBuilder builder = new FlatBufferBuilder(12000);

        List<PhraseTemplatesBlock.WordTemplate> wordTemplates =
                checkNotNull(phraseTemplatesBlock).wordTemplates();

        int[] wordTemplateOffsets = new int[wordTemplates.size()];
        for (int i = 0; i < wordTemplates.size(); i++) {
            PhraseTemplatesBlock.WordTemplate wordTemplate = wordTemplates.get(i);

            int wordTemplateId = wordTemplate.wordTemplateId();
            byte permissions = wordTemplate.permissions();
            byte icon = wordTemplate.icon().code;
            int minLength = wordTemplate.minLength();
            int maxLength = wordTemplate.maxLength();

            int wordTemplateNameOffset = builder.createString(wordTemplate.wordTemplateName());
            int[] symbolSetIds = toIdArray(wordTemplate.symbolSetIds());
            int symbolSetIdsOffset = com.phraser.schema.phraser.WordTemplate.createSymbolSetIdsVector(builder, symbolSetIds);

            int wordTemplateOffset = com.phraser.schema.phraser.WordTemplate.createWordTemplate(builder,
                    wordTemplateId, permissions, icon, minLength, maxLength, wordTemplateNameOffset, symbolSetIdsOffset);

            wordTemplateOffsets[i] = wordTemplateOffset;
        }

        int wordTemplatesOffset =
                com.phraser.schema.phraser.PhraseTemplatesBlock.createWordTemplatesVector(builder, wordTemplateOffsets);

        // -----------------------------------------------------------------------

        List<PhraseTemplatesBlock.PhraseTemplate> phraseTemplates =
                checkNotNull(phraseTemplatesBlock).phraseTemplates();

        int[] phraseTemplateOffsets = new int[phraseTemplates.size()];
        for (int i = 0; i < phraseTemplates.size(); i++) {
            PhraseTemplatesBlock.PhraseTemplate phraseTemplate = phraseTemplates.get(i);

            int phraseTemplateId = phraseTemplate.phraseTemplateId();
            int phraseTemplateNameOffset = builder.createString(phraseTemplate.phraseTemplateName());
            int[] wordTemplateIds = toIdArray(phraseTemplate.wordTemplateIds());

            int wordTemplateIdsOffset = com.phraser.schema.phraser.PhraseTemplate.createWordTemplateIdsVector(builder, wordTemplateIds);

            int phraseTemplateOffset = com.phraser.schema.phraser.PhraseTemplate.createPhraseTemplate(builder,
                    phraseTemplateId, phraseTemplateNameOffset, wordTemplateIdsOffset);

            phraseTemplateOffsets[i] = phraseTemplateOffset;
        }

        int phraseTemplatesOffset =
                com.phraser.schema.phraser.PhraseTemplatesBlock.createPhraseTemplatesVector(builder, phraseTemplateOffsets);

        // -----------------------------------------------------------------------

        com.phraser.schema.phraser.PhraseTemplatesBlock.startPhraseTemplatesBlock(builder);

        int storeBlockOffset = com.phraser.schema.phraser.StoreBlock.createStoreBlock(builder,
                storeBlock.blockId(), storeBlock.version(), storeBlock.entropy());

        com.phraser.schema.phraser.PhraseTemplatesBlock.addBlock(builder, storeBlockOffset);
        com.phraser.schema.phraser.PhraseTemplatesBlock.addPhraseTemplates(builder, phraseTemplatesOffset);
        com.phraser.schema.phraser.PhraseTemplatesBlock.addWordTemplates(builder, wordTemplatesOffset);
        int symbolSetsBlockOffset = com.phraser.schema.phraser.FoldersBlock.endFoldersBlock(builder);

        builder.finish(symbolSetsBlockOffset);

        return builder.sizedByteArray();
    }

    public static byte[] toFlatBufFoldersBlock(FoldersBlock foldersBlock) {
        StoreBlock storeBlock = foldersBlock;
        FlatBufferBuilder builder = new FlatBufferBuilder(12000);

        List<FoldersBlock.Folder> folders = checkNotNull(foldersBlock).folders();
        int[] folderOffsets = new int[folders.size()];
        for (int i = 0; i < folders.size(); i++) {
            FoldersBlock.Folder folder = folders.get(i);

            int folderNameOffset = builder.createString(folder.folderName());

            int folderOffset = com.phraser.schema.phraser.Folder.createFolder(builder,
                    folder.folderId(), folder.parentFolderId(), folderNameOffset);

            folderOffsets[i] = folderOffset;
        }

        int foldersOffset = com.phraser.schema.phraser.FoldersBlock.createFoldersVector(builder, folderOffsets);

        // -----------------------------------------------------------------------

        com.phraser.schema.phraser.FoldersBlock.startFoldersBlock(builder);

        int storeBlockOffset = com.phraser.schema.phraser.StoreBlock.createStoreBlock(builder,
                storeBlock.blockId(), storeBlock.version(), storeBlock.entropy());

        com.phraser.schema.phraser.FoldersBlock.addBlock(builder, storeBlockOffset);
        com.phraser.schema.phraser.FoldersBlock.addFolders(builder, foldersOffset);
        int symbolSetsBlockOffset = com.phraser.schema.phraser.FoldersBlock.endFoldersBlock(builder);

        builder.finish(symbolSetsBlockOffset);

        return builder.sizedByteArray();
    }

    public static byte[] toFlatBufSymbolSetsBlock(SymbolSetsBlock symbolSetsBlock) {
        StoreBlock storeBlock = symbolSetsBlock;
        FlatBufferBuilder builder = new FlatBufferBuilder(12000);

        List<SymbolSetsBlock.SymbolSet> symbolSets = checkNotNull(symbolSetsBlock).symbolSets();
        int[] symbolSetOffsets = new int[symbolSets.size()];
        for (int i = 0; i < symbolSets.size(); i++) {
            SymbolSetsBlock.SymbolSet symbolSet = symbolSets.get(i);

            int symbolSetNameOffset = builder.createString(symbolSet.getName());
            int symbolSetStrOffset = builder.createString(symbolSet.getSymbolSet());
            int symbolSetOffset = com.phraser.schema.phraser.SymbolSet.createSymbolSet(builder, symbolSet.symbolSetId(),
                    symbolSetNameOffset, symbolSetStrOffset);

            symbolSetOffsets[i] = symbolSetOffset;
        }

        int symbolSetsOffset = com.phraser.schema.phraser.SymbolSetsBlock.createSymbolSetsVector(builder, symbolSetOffsets);

        com.phraser.schema.phraser.SymbolSetsBlock.startSymbolSetsBlock(builder);

        int storeBlockOffset = com.phraser.schema.phraser.StoreBlock.createStoreBlock(builder,
                storeBlock.blockId(), storeBlock.version(), storeBlock.entropy());

        com.phraser.schema.phraser.SymbolSetsBlock.addBlock(builder, storeBlockOffset);
        com.phraser.schema.phraser.SymbolSetsBlock.addSymbolSets(builder, symbolSetsOffset);
        int symbolSetsBlockOffset = com.phraser.schema.phraser.SymbolSetsBlock.endSymbolSetsBlock(builder);

        builder.finish(symbolSetsBlockOffset);

        return builder.sizedByteArray();
    }

    public static int[] toIdArray(List<Integer> idList) {
        int[] ids = new int[idList.size()];
        for (int j = 0; j < idList.size(); j++) {
            ids[j] = idList.get(j);
        }
        return ids;
    }
}
