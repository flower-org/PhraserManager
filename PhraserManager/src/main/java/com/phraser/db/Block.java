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
}
