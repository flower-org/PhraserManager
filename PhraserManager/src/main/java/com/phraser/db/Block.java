package com.phraser.db;

import com.google.flatbuffers.FlatBufferBuilder;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/*
  TODO: I guess we can just keep this as an export-only thing

  /** 1 byte * /
  BlockType blockType();

  /** 16 bytes - matches AES data block size, not AES key size * /
  byte[] iv();
  /** 16 byte - adler checksum * /
  int checksum();
*/
@Value.Immutable
public interface Block {
  int FLASH_SECTOR_SIZE = 4096;
  int BLOCK_TYPE_SIZE = 1;
  int IV_SIZE = 16;
  int ADLER_16_SIZE = 2;
  int CHECKSUM_SIZE = ADLER_16_SIZE;
  int BLOCK_REMAINDER_SIZE = FLASH_SECTOR_SIZE - (BLOCK_TYPE_SIZE + IV_SIZE + CHECKSUM_SIZE);
  int BLOCK_DATA_SIZE = (BLOCK_REMAINDER_SIZE / 16) * 16;

  StoreBlock storeBlock();

  default BlockType blockType() {
    if (foldersBlock() != null) {
      return BlockType.FOLDERS_BLOCK;
    } else if (symbolSetsBlock() != null) {
      return BlockType.SYMBOL_SETS_BLOCK;
    } else if (phraseTemplatesBlock() != null) {
      return BlockType.PHRASE_TEMPLATES_BLOCK;
    } else if (phraseBlock() != null) {
      return BlockType.PHRASE_BLOCK;
    } else if (keyBlock() != null) {
      return BlockType.KEY_BLOCK;
    } else {
      throw new RuntimeException("Unknown Block Type");
    }
  }

  default @Nullable FoldersBlock foldersBlock() {
    if (storeBlock() instanceof FoldersBlock) {
      return (FoldersBlock) storeBlock();
    } else {
      return null;
    }
  }

  default @Nullable SymbolSetsBlock symbolSetsBlock() {
    if (storeBlock() instanceof SymbolSetsBlock) {
      return (SymbolSetsBlock) storeBlock();
    } else {
      return null;
    }
  }

  default @Nullable PhraseTemplatesBlock phraseTemplatesBlock() {
    if (storeBlock() instanceof PhraseTemplatesBlock) {
      return (PhraseTemplatesBlock) storeBlock();
    } else {
      return null;
    }
  }

  default @Nullable PhraseBlock phraseBlock() {
    if (storeBlock() instanceof PhraseBlock) {
      return (PhraseBlock) storeBlock();
    } else {
      return null;
    }
  }

  default @Nullable KeyBlock keyBlock() {
    if (storeBlock() instanceof KeyBlock) {
      return (KeyBlock) storeBlock();
    } else {
      return null;
    }
  }

  // --------------------------------------------------

  default BlockType getBlockType() {
    return blockType();
  }

  default int getVersion() {
    return storeBlock().version();
  }

  default int getBlockId() {
    return storeBlock().blockId();
  }

  // --------------------------------------------------

  static Block create(StoreBlock storeBlock) {
    if (!(storeBlock instanceof FoldersBlock) &&
            !(storeBlock instanceof SymbolSetsBlock) &&
            !(storeBlock instanceof PhraseTemplatesBlock) &&
            !(storeBlock instanceof PhraseBlock) &&
            !(storeBlock instanceof KeyBlock)) {
      throw new RuntimeException("Invalid Store Block " + storeBlock.getClass());
    }

    return ImmutableBlock.builder()
            .storeBlock(storeBlock)
            .build();
  }

  default byte[] toFlatBufBlock() {
    BlockType blockType = blockType();
    switch (blockType) {
      case SYMBOL_SETS_BLOCK:
        return toFlatBufSymbolSetsBlock();
      case FOLDERS_BLOCK:
        return toFlatBufFoldersBlock();
      case PHRASE_TEMPLATES_BLOCK:
        return toFlatBufPhraseTemplatesBlock();
      default:
        throw new RuntimeException("Unsupported Block type: " + blockType);
    }
  }

  default byte[] toFlatBufPhraseTemplatesBlock() {
    FlatBufferBuilder builder = new FlatBufferBuilder(12000);

    List<PhraseTemplatesBlock.WordTemplate> wordTemplates =
            checkNotNull(phraseTemplatesBlock()).wordTemplates();

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
            checkNotNull(phraseTemplatesBlock()).phraseTemplates();

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
            blockType().code, storeBlock().blockId(), storeBlock().version(), storeBlock().entropy());

    com.phraser.schema.phraser.PhraseTemplatesBlock.addBlock(builder, storeBlockOffset);
    com.phraser.schema.phraser.PhraseTemplatesBlock.addPhraseTemplates(builder, phraseTemplatesOffset);
    com.phraser.schema.phraser.PhraseTemplatesBlock.addWordTemplates(builder, wordTemplatesOffset);
    int symbolSetsBlockOffset = com.phraser.schema.phraser.FoldersBlock.endFoldersBlock(builder);

    builder.finish(symbolSetsBlockOffset);

    return builder.sizedByteArray();
  }

  default byte[] toFlatBufFoldersBlock() {
    FlatBufferBuilder builder = new FlatBufferBuilder(12000);

    List<FoldersBlock.Folder> folders = checkNotNull(foldersBlock()).folders();
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
            blockType().code, storeBlock().blockId(), storeBlock().version(), storeBlock().entropy());

    com.phraser.schema.phraser.FoldersBlock.addBlock(builder, storeBlockOffset);
    com.phraser.schema.phraser.FoldersBlock.addFolders(builder, foldersOffset);
    int symbolSetsBlockOffset = com.phraser.schema.phraser.FoldersBlock.endFoldersBlock(builder);

    builder.finish(symbolSetsBlockOffset);

    return builder.sizedByteArray();
  }

  default byte[] toFlatBufSymbolSetsBlock() {
    FlatBufferBuilder builder = new FlatBufferBuilder(12000);

    List<SymbolSetsBlock.SymbolSet> symbolSets = checkNotNull(symbolSetsBlock()).symbolSets();
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
            blockType().code, storeBlock().blockId(), storeBlock().version(), storeBlock().entropy());

    com.phraser.schema.phraser.SymbolSetsBlock.addBlock(builder, storeBlockOffset);
    com.phraser.schema.phraser.SymbolSetsBlock.addSymbolSets(builder, symbolSetsOffset);
    int symbolSetsBlockOffset = com.phraser.schema.phraser.SymbolSetsBlock.endSymbolSetsBlock(builder);

    builder.finish(symbolSetsBlockOffset);

    return builder.sizedByteArray();
  }

  static int[] toIdArray(List<Integer> idList) {
    int[] ids = new int[idList.size()];
    for (int j = 0; j < idList.size(); j++) {
      ids[j] = idList.get(j);
    }
    return ids;
  }
}
