package com.phraser.db;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.BlockType.KEY_BLOCK;
import static com.phraser.db.BlockType.PHRASE_BLOCK;

public class PhraserDB {
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
                ", since db currently contains " + dbBlocks.size() + " blocks. Try compacting.");
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

  public boolean isInvalid(Block dbBlock) {
    return !isLatest(dbBlock) ||
            (dbBlock.blockType() == PHRASE_BLOCK && checkNotNull(dbBlock.phraseBlock()).isTombstone());
  }

  public long getLastBlockVersion(int blockId) {
    return checkNotNull(lastBlockByBlockId.get(blockId)).getVersion();
  }

  public @Nullable Block getLastBlock(int blockId) {
    return lastBlockByBlockId.get(blockId);
  }

  public void compact() {
    List<Block> validBlocks = new ArrayList<>();
    for (Block block : dbBlocks) {
      if (!isInvalid(block)) {
        validBlocks.add(block);
      }
    }
    dbBlocks.clear();
    dbBlocks.addAll(validBlocks);
  }
}
