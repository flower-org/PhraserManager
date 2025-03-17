package com.phraser.runtimedb;

import com.phraser.db.Block;
import com.phraser.db.BlockType;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.dbcodec.BlockData;
import com.phraser.dbcodec.ChecksumException;
import com.phraser.dbcodec.DbEncoder;
import com.phraser.dbcodec.DbFileManager;
import com.phraser.dbcodec.FlatBufBlockDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.Block.FLASH_SECTOR_SIZE;
import static com.phraser.db.PhraseTemplatesBlock.PhraseTemplate;
import static com.phraser.db.PhraseTemplatesBlock.WordTemplate;
import static com.phraser.db.SymbolSetsBlock.SymbolSet;
import static com.phraser.dbcodec.DbFileManager.HARDCODED_IV_MASK;

/** Mimics DB data structures and related logic the way it will operate on a microcontroller */
public class DbRuntime {
    final static Logger LOGGER = LoggerFactory.getLogger(DbRuntime.class);

    public static class BlockNumberAndVersion {
        public final int blockNumber;
        public final long version;
        public BlockNumberAndVersion(int blockNumber, long version) {
            this.blockNumber = blockNumber;
            this.version = version;
        }
    }
    public static class PhraseFolderAndName {
        public final int phraseBlockId;
        public final int folderId;
        public final String name;

        public PhraseFolderAndName(int phraseBlockId, int folderId, String name) {
            this.phraseBlockId = phraseBlockId;
            this.folderId = folderId;
            this.name = name;
        }
    }

    final File dbFile;
    final RandomAccessFile f;
    final String dbPassword;
    final String dbName;

    int lastBlockId = 0;
    long lastBlockVersion = 0;

    final int keyBlockId;
    final int foldersBlockId;
    final int phraseTemplatesBlockId;
    final int symbolSetsBlockId;

    final byte[] aes256Key;
    final byte[] aes256IvMask;

    final TreeMap<Integer, Integer> occupiedBlocksNumbers;
    final Map<Integer, BlockNumberAndVersion> blockNumberAndVersionByBlockId;

    //Cached metadata blocks
    final Map<Integer, PhraseTemplate> phraseTemplates;
    final Map<Integer, WordTemplate> wordTemplates;
    final Map<Integer, SymbolSet> symbolSets;
    final Map<Integer, String> folders;
    final Map<Integer, Set<Integer>> subFoldersByFolder;
    final Map<Integer, Set<PhraseFolderAndName>> phrasesByFolder;

    static void readFileAtPos(byte[] bytes, RandomAccessFile f, int positionInFile) throws IOException {
        f.seek(positionInFile);
        f.read(bytes, 0, bytes.length);
    }

    public DbRuntime(File dbFile, String dbPassword) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        this.dbFile = dbFile;
        this.dbPassword = dbPassword;

        this.f = new RandomAccessFile(dbFile, "rwd");
        this.occupiedBlocksNumbers = new TreeMap<>();
        this.blockNumberAndVersionByBlockId = new HashMap<>();

        byte[] block = new byte[FLASH_SECTOR_SIZE];

        // 1. find latest KeyBlock
        Block latestKeyBlock = null;
        int latestKeyBlockNumber = -1;

        byte[] keyBlockKey = DbFileManager.getPbkdf2Key(dbPassword);
        for (int i = 0; i < f.length(); i+=FLASH_SECTOR_SIZE) {
            readFileAtPos(block, f, i);

            BlockData blockData = null;
            try {
                blockData = DbEncoder.decodeBlock(block, keyBlockKey, HARDCODED_IV_MASK);
            } catch (ChecksumException e) {
                LOGGER.trace("Block checksum failed", e);
            } catch (Exception e) {
                LOGGER.error("Block decoding issue", e);
            }
            if (blockData != null) {
                if (blockData.blockType == BlockType.KEY_BLOCK) {
                    Block keyBlock = Block.of(FlatBufBlockDecoder.fromFlatBufKeyBlock(blockData.blockData));

                    lastBlockId = Math.max(lastBlockId, keyBlock.getBlockId());
                    lastBlockVersion = Math.max(lastBlockVersion, keyBlock.getVersion());

                    if (latestKeyBlock == null || latestKeyBlock.getVersion() < checkNotNull(keyBlock.keyBlock()).version()) {
                        latestKeyBlock = keyBlock;
                        latestKeyBlockNumber = i / FLASH_SECTOR_SIZE;
                    }
                }
            }
        }
        if (latestKeyBlock == null) {
            throw new RuntimeException("Failed to decrypt KeyBlock");
        }
        KeyBlock keyBlock = checkNotNull(latestKeyBlock.keyBlock());
        keyBlockId = keyBlock.blockId();
        aes256Key = keyBlock.key();
        aes256IvMask = keyBlock.iv();
        dbName = keyBlock.dbName();
        blockNumberAndVersionByBlockId.put(keyBlockId, new BlockNumberAndVersion(latestKeyBlockNumber, keyBlock.version()));

        // 2. scan other blocks to find all latest versions
        Integer localFoldersBlockId = null;
        Integer localPhraseTemplatesBlockId = null;
        Integer localSymbolSetsBlockId = null;
        Map<Integer, PhraseFolderAndName> phraseFolders = new HashMap<>();

        Set<Integer> tombstonedPhraseBlocks = new HashSet<>();
        for (int i = 0; i < f.length(); i+=FLASH_SECTOR_SIZE) {
            readFileAtPos(block, f, i);

            BlockData blockData = null;
            try {
                blockData = DbEncoder.decodeBlock(block, aes256Key, aes256IvMask);
            } catch (ChecksumException e) {
                LOGGER.trace("Block checksum failed", e);
            } catch (Exception e) {
                LOGGER.error("Block decoding issue", e);
            }
            if (blockData != null) {
                Block newBlock;
                switch (blockData.blockType) {
                    case KEY_BLOCK: newBlock = Block.of(FlatBufBlockDecoder.fromFlatBufKeyBlock(blockData.blockData)); break;
                    case SYMBOL_SETS_BLOCK: newBlock = Block.of(FlatBufBlockDecoder.fromFlatBufSymbolSetsBlock(blockData.blockData)); break;
                    case FOLDERS_BLOCK: newBlock = Block.of(FlatBufBlockDecoder.fromFlatBufFoldersBlock(blockData.blockData)); break;
                    case PHRASE_TEMPLATES_BLOCK: newBlock = Block.of(FlatBufBlockDecoder.fromFlatBufPhraseTemplatesBlock(blockData.blockData)); break;
                    case PHRASE_BLOCK: newBlock = Block.of(FlatBufBlockDecoder.fromFlatBufPhraseBlock(blockData.blockData)); break;
                    default: throw new RuntimeException("Unexpected block type " + blockData.blockType);
                }

                int newBlockId = newBlock.getBlockId();
                long newBlockVersion = newBlock.getVersion();

                lastBlockId = Math.max(lastBlockId, newBlockId);
                lastBlockVersion = Math.max(lastBlockVersion, newBlockVersion);

                BlockNumberAndVersion old = blockNumberAndVersionByBlockId.get(newBlockId);
                if (old == null || old.version < newBlockVersion) {
                    int blockNumber = i / FLASH_SECTOR_SIZE;

                    //Ignore tombstoned phrase blocks
                    if (newBlock.blockType() == BlockType.PHRASE_BLOCK && !checkNotNull(newBlock.phraseBlock()).isTombstone()) {
                        tombstonedPhraseBlocks.add(newBlockId);
                        blockNumberAndVersionByBlockId.remove(newBlockId);
                        phraseFolders.remove(newBlockId);
                        continue;
                    }
                    if (tombstonedPhraseBlocks.contains(newBlockId)) {
                        continue;
                    }

                    blockNumberAndVersionByBlockId.put(newBlockId, new BlockNumberAndVersion(blockNumber, newBlockVersion));
                    if (newBlock.blockType() == BlockType.SYMBOL_SETS_BLOCK) {
                        localSymbolSetsBlockId = newBlockId;
                    } else if (newBlock.blockType() == BlockType.FOLDERS_BLOCK) {
                        localFoldersBlockId = newBlockId;
                    } else if (newBlock.blockType() == BlockType.PHRASE_TEMPLATES_BLOCK) {
                        localPhraseTemplatesBlockId = newBlockId;
                    } else if (newBlock.blockType() == BlockType.PHRASE_BLOCK) {
                        phraseFolders.put(newBlockId,
                                            new PhraseFolderAndName(newBlockId,
                                                checkNotNull(newBlock.phraseBlock()).folderId(),
                                                checkNotNull(newBlock.phraseBlock()).phraseName()));
                    }
                }
            }
        }

        if (localFoldersBlockId == null) {
            throw new RuntimeException("Failed to find FoldersBlock");
        }
        if (localPhraseTemplatesBlockId == null) {
            throw new RuntimeException("Failed to find PhraseTemplatesBlock");
        }
        if (localSymbolSetsBlockId == null) {
            throw new RuntimeException("Failed to find SymbolSetsBlock");
        }

        foldersBlockId = localFoldersBlockId;
        phraseTemplatesBlockId = localPhraseTemplatesBlockId;
        symbolSetsBlockId = localSymbolSetsBlockId;

        // 3. Fill phrasesByFolder
        phrasesByFolder = new HashMap<>();
        phraseFolders.forEach(
            (phraseIdAndName, phraseFolderAndName)
                -> phrasesByFolder.computeIfAbsent(phraseFolderAndName.folderId, k -> new HashSet<>()).add(phraseFolderAndName));

        // 4. Fill occupied blocks
        for (BlockNumberAndVersion bnv : blockNumberAndVersionByBlockId.values()) {
            occupiedBlocksNumbers.put(bnv.blockNumber, bnv.blockNumber);
        }

        // 5. Fill metadata caches
        {
            // 5.1 PhraseTemplates Cache
            int phraseTemplatesBlockNumber = checkNotNull(blockNumberAndVersionByBlockId.get(phraseTemplatesBlockId)).blockNumber;
            int phraseTemplatesBlockPosition = phraseTemplatesBlockNumber * FLASH_SECTOR_SIZE;
            readFileAtPos(block, f, phraseTemplatesBlockPosition);
            BlockData blockData = DbEncoder.decodeBlock(block, aes256Key, aes256IvMask);
            Block phraseTemplatesBlock = Block.of(FlatBufBlockDecoder.fromFlatBufPhraseTemplatesBlock(blockData.blockData));
            assert (phraseTemplatesBlock.blockType() == BlockType.PHRASE_TEMPLATES_BLOCK);

            phraseTemplates = new HashMap<>();
            checkNotNull(phraseTemplatesBlock.phraseTemplatesBlock()).phraseTemplates()
                    .forEach(pt -> phraseTemplates.put(pt.phraseTemplateId(), pt));
            wordTemplates = new HashMap<>();
            checkNotNull(phraseTemplatesBlock.phraseTemplatesBlock()).wordTemplates()
                    .forEach(wt -> wordTemplates.put(wt.wordTemplateId(), wt));
        }

        {
            // 5.2 SymbolSets Cache
            int symbolSetsBlockNumber = checkNotNull(blockNumberAndVersionByBlockId.get(symbolSetsBlockId)).blockNumber;
            int symbolSetsBlockPosition = symbolSetsBlockNumber * FLASH_SECTOR_SIZE;
            readFileAtPos(block, f, symbolSetsBlockPosition);
            BlockData blockData = DbEncoder.decodeBlock(block, aes256Key, aes256IvMask);
            Block symbolSetsBlock = Block.of(FlatBufBlockDecoder.fromFlatBufSymbolSetsBlock(blockData.blockData));
            assert (symbolSetsBlock.blockType() == BlockType.SYMBOL_SETS_BLOCK);

            symbolSets = new HashMap<>();
            checkNotNull(symbolSetsBlock.symbolSetsBlock()).symbolSets()
                    .forEach(ss -> symbolSets.put(ss.symbolSetId(), ss));
        }

        {
            // 5.3 Folders cache
            int foldersBlockNumber = checkNotNull(blockNumberAndVersionByBlockId.get(foldersBlockId)).blockNumber;
            int foldersBlockPosition = foldersBlockNumber * FLASH_SECTOR_SIZE;
            readFileAtPos(block, f, foldersBlockPosition);
            BlockData blockData = DbEncoder.decodeBlock(block, aes256Key, aes256IvMask);
            Block foldersBlock = Block.of(FlatBufBlockDecoder.fromFlatBufFoldersBlock(blockData.blockData));
            assert (foldersBlock.blockType() == BlockType.FOLDERS_BLOCK);

            folders = new HashMap<>();
            subFoldersByFolder = new HashMap<>();
            checkNotNull(foldersBlock.foldersBlock()).folders()
                    .forEach(f -> {
                        folders.put(f.folderId(), f.folderName());
                        subFoldersByFolder.computeIfAbsent(f.parentFolderId(), k -> new HashSet<>())
                                .add(f.folderId());
                    });
        }
    }

    public @Nullable PhraseBlock getPhrase(int phraseBlockId) {
        throw new UnsupportedOperationException();
    }

    public @Nullable PhraseTemplate getPhraseTemplate(int phraseTemplateId) {
        return phraseTemplates.get(phraseTemplateId);
    }

    public @Nullable WordTemplate getWordTemplate(int wordTemplateId) {
        return wordTemplates.get(wordTemplateId);
    }

    public @Nullable SymbolSet getSymbolSet(int symbolSetId) {
        return symbolSets.get(symbolSetId);
    }

    public String getDbName() {
        return dbName;
    }
}
