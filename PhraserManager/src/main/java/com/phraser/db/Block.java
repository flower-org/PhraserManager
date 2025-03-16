package com.phraser.db;

import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
public interface Block {
  int FLASH_SECTOR_SIZE = 4096;
  int IV_SIZE = 16;
  int ADLER_32_CHECKSUM_SIZE = 4;
  int BLOCK_TYPE_SIZE = 1;
  int DATA_LENGTH_SIZE = 2;
  int DATA_BLOCK_SIZE = FLASH_SECTOR_SIZE - (IV_SIZE + ADLER_32_CHECKSUM_SIZE + BLOCK_TYPE_SIZE + DATA_LENGTH_SIZE);
  int ENCRYPTED_BLOCK_SIZE_NO_ADLER = FLASH_SECTOR_SIZE - (IV_SIZE + ADLER_32_CHECKSUM_SIZE);
  int ENCRYPTED_BLOCK_SIZE = FLASH_SECTOR_SIZE - IV_SIZE;

  Block DUMMY = ImmutableBlock.builder().storeBlock(StoreBlock.of(0,0,0)).build();

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

  default long getVersion() {
    return storeBlock().version();
  }

  default int getBlockId() {
    return storeBlock().blockId();
  }

  default long getEntropy() {
    return storeBlock().entropy();
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
}
