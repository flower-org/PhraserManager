package com.phraser.db;

import com.google.flatbuffers.FlatBufferBuilder;
import com.phraser.schema.phraser.BlockType;
import com.phraser.schema.phraser.FoldersBlock;
import com.phraser.schema.phraser.KeyBlock;
import com.phraser.schema.phraser.PhraseBlock;
import com.phraser.schema.phraser.PhraseTemplatesBlock;
import com.phraser.schema.phraser.StoreBlock;
import com.phraser.schema.phraser.SymbolSetsBlock;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
public interface Block {
  int FLASH_SECTOR_SIZE = 4096;
  int IV_SIZE = 16;
  int ADLER_SIZE = 2;
  int BLOCK_DATA_SIZE = FLASH_SECTOR_SIZE - (IV_SIZE + ADLER_SIZE);

  @Nullable Integer adler16Checksum();
  @Nullable byte[] blockIv();

  @Nullable FoldersBlock foldersBlock();
  @Nullable SymbolSetsBlock symbolSetsBlock();
  @Nullable PhraseTemplatesBlock phraseTemplatesBlock();
  @Nullable PhraseBlock phraseBlock();
  @Nullable KeyBlock keyBlock();

  static Block create(FoldersBlock foldersBlock) {
    return ImmutableBlock.builder()
      .foldersBlock(foldersBlock)
      .build();
  }
  static Block create(SymbolSetsBlock symbolSetsBlock) {
    return ImmutableBlock.builder()
      .symbolSetsBlock(symbolSetsBlock)
      .build();
  }
  static Block create(PhraseTemplatesBlock phraseTemplatesBlock) {
    return ImmutableBlock.builder()
      .phraseTemplatesBlock(phraseTemplatesBlock)
      .build();
  }
  static Block create(PhraseBlock phraseBlock) {
    return ImmutableBlock.builder()
      .phraseBlock(phraseBlock)
      .build();
  }
  static Block create(KeyBlock keyBlock) {
    return ImmutableBlock.builder()
      .keyBlock(keyBlock)
      .build();
  }

  static KeyBlock createFirstKeyBlock(byte[] key_256, byte[] iv_128) {
    assert(key_256.length == 32);
    assert(iv_128.length == 16);

    FlatBufferBuilder flatBufferBuilder = new FlatBufferBuilder(BLOCK_DATA_SIZE);
    int baseBlockOffset = StoreBlock.createStoreBlock(flatBufferBuilder, 1, 1, BlockType.KeyBlock, System.nanoTime());
    int keyOffset = flatBufferBuilder.createByteVector(key_256);
    int ivOffset = flatBufferBuilder.createByteVector(iv_128);

    KeyBlock.startKeyBlock(flatBufferBuilder);
    KeyBlock.addBlock(flatBufferBuilder, baseBlockOffset);
    KeyBlock.addKey(flatBufferBuilder, keyOffset);
    KeyBlock.addIv(flatBufferBuilder, ivOffset);
    int keyBlockOffset = KeyBlock.endKeyBlock(flatBufferBuilder);

    flatBufferBuilder.finish(keyBlockOffset);
    return KeyBlock.getRootAsKeyBlock(flatBufferBuilder.dataBuffer());
  }
}
