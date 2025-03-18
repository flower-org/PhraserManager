package com.phraser.db;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.Block.FLASH_SECTOR_SIZE;
import static com.phraser.db.BlockType.KEY_BLOCK;

public class PhraserDB {
  static final byte[] DEFAULT_IV =
    new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };

  //AES-256 key
  static final byte[] DEFAULT_KEY =
    new byte[] { 0x60, 0x3d, (byte)0xeb, 0x10, 0x15, (byte)0xca, 0x71, (byte)0xbe, 0x2b, 0x73, (byte)0xae, (byte)0xf0, (byte)0x85, 0x7d, 0x77, (byte)0x81,
        0x1f, 0x35, 0x2c, 0x07, 0x3b, 0x61, 0x08, (byte)0xd7, 0x2d, (byte)0x98, 0x10, (byte)0xa3, 0x09, 0x14, (byte)0xdf, (byte)0xf4 };

  public static final int BLOCKS_IN_DB = (1024 * 1024) / FLASH_SECTOR_SIZE; // 256 blocks in 1 mb

  @Nullable Block lastKeyBlock = null;
  @Nullable Block lastSymbolSetBlock = null;
  @Nullable Block lastFoldersBlock = null;
  @Nullable Block lastPhraseTemplatesBlock = null;

  Map<Integer, Block> lastBlockByBlockId = new HashMap<>();
  final ObservableList<Block> dbBlocks;

  @Nullable String dbName;

  long blockCount;

  int lastBlockId = 0;
  long lastVersion = 0;

  @Nullable Consumer<String> dbNameListener;

  public PhraserDB(List<Block> blocks, int blockCount, @Nullable Consumer<String> dbNameListener) {
    if (blocks.size() > blockCount) {
      throw new RuntimeException("blocks.length > blockCount");
    }
    this.dbNameListener = dbNameListener;
    this.blockCount = blockCount;
    dbBlocks = FXCollections.observableArrayList();

    for (Block block : blocks) {
      addBlock(block);
    }
  }

  public ObservableList<Block> blocksObservableArray() {
    return dbBlocks;
  }

  protected @Nullable Integer getNextOverwritableBlockIndex() {
    if (!dbBlocks.isEmpty()) {
      // 1. If we still have space in our blocks, use next available spot
      if (dbBlocks.size() < blockCount) {
        return dbBlocks.size();
      }

      // 2. Otherwise, we start with finding the latest block (index)
      Block latestBlock = dbBlocks.get(0);
      int latestBlockIndex = 0;
      for (int i = 0; i < dbBlocks.size(); i++) {
        Block dbBlock = dbBlocks.get(i);
        if (dbBlock.getVersion() > latestBlock.getVersion()) {
          latestBlock = dbBlock;
          latestBlockIndex = i;
        }
      }

      // 3. Find the next over-writable block "to the right" from the latest block
      for (int i = 0; i < dbBlocks.size(); i++) {
        int nextWritableBlockIndex = (i + 1 + latestBlockIndex) % dbBlocks.size();
        Block overwriteCandidate = dbBlocks.get(nextWritableBlockIndex);
        Block candidateLatestVersion = checkNotNull(getLastBlock(overwriteCandidate.getBlockId()));

        boolean isOldVersion = overwriteCandidate.getVersion() < candidateLatestVersion.getVersion();
        boolean isTombstone = candidateLatestVersion.blockType() == BlockType.PHRASE_BLOCK &&
                checkNotNull(candidateLatestVersion.phraseBlock()).isTombstone();
        if (isOldVersion || isTombstone) {
          return nextWritableBlockIndex;
        }
      }

      // 4. If not found, it means that we're out of space
      return null;
    } else {
      // 0. If dbBlocks list is empty, use index 0
      return 0;
    }
  }

  public void addBlock(Block block) {
    if (block.blockType() == KEY_BLOCK) {
      int newBlockCount = checkNotNull(block.keyBlock()).blockCount();
      if (newBlockCount < dbBlocks.size()) {
        throw new RuntimeException("Can't reduce block count to " + block.keyBlock().blockCount() +
                ", db currently contains " + dbBlocks.size() + "blocks. Try defragmenting.");
      }
      blockCount = newBlockCount;

      notifyDbName();
     }

    // 1. add block to dbBlocks list
    Integer nextOverwritableBlockIndex = getNextOverwritableBlockIndex();

    // If there are no overwritable blocks, the only way to do this is to overwrite in-place
    if (nextOverwritableBlockIndex == null) {
      Block blocksLatestVersion = getLastBlock(block.getBlockId());
      // Find index of block's last version
      if (blocksLatestVersion != null) {
        for (int i = 0; i < dbBlocks.size(); i++) {
          Block dbBlock = dbBlocks.get(i);
          if (blocksLatestVersion.getVersion() == dbBlock.getVersion()) {
           nextOverwritableBlockIndex = i;
           break;
          }
        }
      }
    }

    // If there are no overwritable blocks, and it's a new block, we throw Out Of Capacity error
    if (nextOverwritableBlockIndex == null) {
      throw new RuntimeException("No spare blocks left (" + dbBlocks.size() + "/" + blockCount + ")");
    }

    // Write to the blocklist index
    if (dbBlocks.size() < blockCount) {
      dbBlocks.add(block);
    } else {
      dbBlocks.set(nextOverwritableBlockIndex, block);
    }

    // 2. Update DB stats
    lastBlockId = Math.max(lastBlockId, block.getBlockId());
    lastVersion = Math.max(lastVersion, block.getVersion());

    Block previousBlock = lastBlockByBlockId.get(block.getBlockId());
    if (previousBlock == null || previousBlock.getVersion() < block.getVersion()) {
      lastBlockByBlockId.put(block.getBlockId(), block);
    }

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
    } else if (block.keyBlock() != null) {
      com.phraser.db.KeyBlock keyBlock = block.keyBlock();
      if (lastKeyBlock == null || keyBlock.version() > lastKeyBlock.getVersion()) {
        lastKeyBlock = block;
        setDbName(keyBlock.dbName());
      }
    }
  }

  protected void notifyDbName() {
    if (dbNameListener != null) {
      if (!StringUtils.isBlank(dbName)) {
        dbNameListener.accept(dbName);
      } else {
        dbNameListener.accept("Untitled");
      }
    }
  }

  protected void setDbName(@Nullable String newDbName) {
    dbName = newDbName;
    notifyDbName();
  }

  public void setDbNameListener(@Nullable Consumer<String> dbNameListener) {
    this.dbNameListener = dbNameListener;
    notifyDbName();
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

  @Nullable
  public String dbName() {
    return dbName;
  }

  public int incrementAndGetBlockId() { return ++lastBlockId; }
  public long incrementAndGetVersion() { return ++lastVersion; }
  public long getLastVersion() { return lastVersion; }

  public boolean isLatest(Block dbBlock) {
    Block block = lastBlockByBlockId.get(dbBlock.getBlockId());
    if (block != null) {
      return dbBlock.getVersion() >= block.getVersion();
    }
    return true;
  }

  public long getLastBlockVersion(int blockId) {
    return checkNotNull(lastBlockByBlockId.get(blockId)).getVersion();
  }

  public @Nullable Block getLastBlock(int blockId) {
    return lastBlockByBlockId.get(blockId);
  }
}
