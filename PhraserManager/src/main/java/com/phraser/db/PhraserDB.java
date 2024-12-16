package com.phraser.db;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.phraser.db.Block.FLASH_SECTOR_SIZE;

public class PhraserDB {
  static final byte[] DEFAULT_IV =
    new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };

  //AES-256 key
  static final byte[] DEFAULT_KEY =
    new byte[] { 0x60, 0x3d, (byte)0xeb, 0x10, 0x15, (byte)0xca, 0x71, (byte)0xbe, 0x2b, 0x73, (byte)0xae, (byte)0xf0, (byte)0x85, 0x7d, 0x77, (byte)0x81,
        0x1f, 0x35, 0x2c, 0x07, 0x3b, 0x61, 0x08, (byte)0xd7, 0x2d, (byte)0x98, 0x10, (byte)0xa3, 0x09, 0x14, (byte)0xdf, (byte)0xf4 };

  public static final int BLOCKS_IN_DB = (1024 * 1024) / FLASH_SECTOR_SIZE; // 256 blocks

  @Nullable Block lastKeyBlock = null;
  @Nullable Block lastSymbolSetBlock = null;
  @Nullable Block lastFoldersBlock = null;
  @Nullable Block lastPhraseTemplatesBlock = null;
  final Map<Integer, Block> lastPhraseBlocks;
  @Nullable String dbName;

  final long bucketCount;

  int lastBlockVersion = 0;
  int bucketCursor = 0;

  @Nullable final Consumer<String> dbNameListener;

  public static PhraserDB createNewDb(int bucketCount, String dbName, @Nullable Consumer<String> dbNameListener) {
    return new PhraserDB(List.of(Block.create(KeyBlock.createFirstKeyBlock(DEFAULT_KEY, 1))), bucketCount, dbName, dbNameListener);
  }

  final Block[] blocks;

  public PhraserDB(List<Block> blocks, int bucketCount, @Nullable String defaultDbName, @Nullable Consumer<String> dbNameListener) {
    if (blocks.size() > bucketCount) {
      throw new RuntimeException("blocks.length > bucketCount");
    }
    this.dbName = defaultDbName;
    this.dbNameListener = dbNameListener;
    this.bucketCount = bucketCount;
    this.blocks = new Block[bucketCount];
    this.lastPhraseBlocks = new HashMap<>();

    for (Block block : blocks) {
      addBlock(block);
    }
  }

  public Block[] blocks() {
    return blocks;
  }

  public void addBlock(Block block) {
    lastBlockVersion = Math.min(lastBlockVersion, block.getVersion());
    blocks[bucketCursor++] = block;

    if (block.foldersBlock() != null) {
      com.phraser.db.FoldersBlock foldersBlock = block.foldersBlock();
      if (lastFoldersBlock == null || foldersBlock.version() > lastFoldersBlock.getVersion()) {
        lastFoldersBlock = block;
      }
    } else if (block.symbolSetsBlock() != null) {
      com.phraser.db.SymbolSetsBlock symbolSetsBlock = block.symbolSetsBlock();
      if (lastSymbolSetBlock == null || symbolSetsBlock.version() > lastSymbolSetBlock.getVersion()) {
        lastSymbolSetBlock = block;
      }
    } else if (block.phraseTemplatesBlock() != null) {
      com.phraser.db.PhraseTemplatesBlock phraseTemplatesBlock = block.phraseTemplatesBlock();
      if (lastPhraseTemplatesBlock == null || phraseTemplatesBlock.version() > lastPhraseTemplatesBlock.getVersion()) {
        lastPhraseTemplatesBlock = block;
      }
    } else if (block.phraseBlock() != null) {
      com.phraser.db.PhraseBlock phraseBlock = block.phraseBlock();
      Block oldPhraseBlock = lastPhraseBlocks.get(phraseBlock.blockId());
      if (oldPhraseBlock == null || phraseBlock.version() > oldPhraseBlock.getVersion()) {
        lastPhraseBlocks.put(phraseBlock.blockId(), block);
      }
    } else if (block.keyBlock() != null) {
      com.phraser.db.KeyBlock keyBlock = block.keyBlock();
      if (lastKeyBlock == null || keyBlock.version() > lastKeyBlock.getVersion()) {
        lastKeyBlock = block;
        setDbName(keyBlock.dbName());
      }
    }
  }

  void setDbName(@Nullable String newDbName) {
    dbName = newDbName;
    if (dbNameListener != null) {
      dbNameListener.accept(dbName);
    }
  }

  public int totalBlockCountIncludingEmpty() {
    return blocks.length;
  }

  public int nonEmptyBlockCountIncludingOldVersions() { throw new UnsupportedOperationException(); }

  public int uniqueBlockCount() {
    throw new UnsupportedOperationException();
  }

  public void loadFromFile(File file) {
    throw new UnsupportedOperationException();
  }

  public void saveToFile(File file) {
    throw new UnsupportedOperationException();
  }

  public void defragment() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  public Block getLastKeyBlock() {
    return lastKeyBlock;
  }

  @Nullable
  public Block getLastSymbolSetBlock() {
    return lastSymbolSetBlock;
  }

  @Nullable
  public Block getLastFoldersBlock() {
    return lastFoldersBlock;
  }

  @Nullable
  public Block getLastPhraseTemplatesBlock() {
    return lastPhraseTemplatesBlock;
  }

  public Map<Integer, Block> getLastPhraseBlocks() {
    return lastPhraseBlocks;
  }

  @Nullable
  public String dbName() {
    return dbName;
  }

  public int getNextBlockId() {
    lastBlockVersion++;
    return lastBlockVersion;
  }
}
