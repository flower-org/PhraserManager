package com.phraser.runtimedb;

import com.phraser.db.Block;
import com.phraser.db.BlockType;
import com.phraser.db.ImmutableFoldersBlock;
import com.phraser.db.ImmutableKeyBlock;
import com.phraser.db.ImmutablePhraseBlock;
import com.phraser.db.ImmutablePhraseTemplatesBlock;
import com.phraser.db.ImmutableSymbolSetsBlock;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.dbcodec.BlockData;
import com.phraser.dbcodec.ChecksumException;
import com.phraser.dbcodec.DbEncoder;
import com.phraser.dbcodec.FlatBufBlockDecoder;
import com.phraser.dbcodec.FlatBufBlockEncoder;
import com.phraser.utils.Pbkdf2Tool;
import com.phraser.utils.TreeUtil;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.Block.FLASH_SECTOR_SIZE;
import static com.phraser.db.FoldersBlock.Folder;
import static com.phraser.db.PhraseTemplatesBlock.PhraseTemplate;
import static com.phraser.db.PhraseTemplatesBlock.WordTemplate;
import static com.phraser.db.SymbolSetsBlock.SymbolSet;
import static com.phraser.utils.Pbkdf2Tool.HARDCODED_IV_MASK;

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

    public static class FolderContent {
        public final List<Folder> subFolders;
        public final List<PhraseFolderAndName> phrases;

        public FolderContent(List<Folder> subFolders, List<PhraseFolderAndName> phrases) {
            this.subFolders = subFolders;
            this.phrases = phrases;
        }
    }

    final File dbFile;
    final RandomAccessFile f;
    final String dbPassword;
    final String dbName;

    int lastBlockId = 0;
    long lastBlockVersion = 0;
    int lastBlockNumber = 0;

    final int keyBlockId;
    final int blockCount;
    final int foldersBlockId;
    final int phraseTemplatesBlockId;
    final int symbolSetsBlockId;

    final byte[] keyBlockKey;
    final byte[] aes256Key;
    final byte[] aes256IvMask;

    final TreeMap<Integer, Integer> occupiedBlocksNumbers;
    final Map<Integer, BlockNumberAndVersion> blockNumberAndVersionByBlockId;

    //Cached metadata blocks
    // - SymbolSetsBlock cache
    final Map<Integer, SymbolSet> symbolSets;

    // - FoldersBlock cache
    final Map<Integer, Folder> folders;
    final Map<Integer, Set<Integer>> subFoldersByFolder;

    // - PhraseTemplatesBlock cache
    final Map<Integer, PhraseTemplate> phraseTemplates;
    final Map<Integer, WordTemplate> wordTemplates;

    // - Phrase Blocks (minimal info) cache
    final Map<Integer, PhraseFolderAndName> phrases;
    final Map<Integer, Set<Integer>> phrasesByFolder;

    static void readFromFileAtPos(byte[] bytes, RandomAccessFile f, int positionInFile) throws IOException {
        f.seek(positionInFile);
        f.read(bytes, 0, bytes.length);
    }

    static void writeToFileAtPos(byte[] bytes, RandomAccessFile f, int positionInFile) throws IOException {
        f.seek(positionInFile);
        f.write(bytes, 0, bytes.length);
    }

    public static Block fromBlockData(BlockData blockData) {
        switch (blockData.blockType) {
            case KEY_BLOCK:
                return Block.of(FlatBufBlockDecoder.fromFlatBufKeyBlock(blockData.blockData));
            case SYMBOL_SETS_BLOCK:
                return Block.of(FlatBufBlockDecoder.fromFlatBufSymbolSetsBlock(blockData.blockData));
            case FOLDERS_BLOCK:
                return Block.of(FlatBufBlockDecoder.fromFlatBufFoldersBlock(blockData.blockData));
            case PHRASE_TEMPLATES_BLOCK:
                return Block.of(FlatBufBlockDecoder.fromFlatBufPhraseTemplatesBlock(blockData.blockData));
            case PHRASE_BLOCK:
                return Block.of(FlatBufBlockDecoder.fromFlatBufPhraseBlock(blockData.blockData));
            default:
                throw new RuntimeException("Unexpected block type " + blockData.blockType);
        }
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

        keyBlockKey = Pbkdf2Tool.getPbkdf2Key(dbPassword);
        for (int i = 0; i < f.length(); i += FLASH_SECTOR_SIZE) {
            readFromFileAtPos(block, f, i);

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
                    if (lastBlockVersion < keyBlock.getVersion()) {
                        lastBlockVersion = keyBlock.getVersion();
                        lastBlockNumber = i / FLASH_SECTOR_SIZE;
                    }

                    if (latestKeyBlock == null || latestKeyBlock.getVersion() < checkNotNull(keyBlock.keyBlock()).version()) {
                        latestKeyBlock = keyBlock;
                        latestKeyBlockNumber = i / FLASH_SECTOR_SIZE;
                    }
                }
            }
        }
        if (latestKeyBlock == null) { throw new RuntimeException("Failed to decrypt KeyBlock"); }
        KeyBlock keyBlock = checkNotNull(latestKeyBlock.keyBlock());
        keyBlockId = keyBlock.blockId();
        blockCount = keyBlock.blockCount();
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
        for (int i = 0; i < f.length(); i += FLASH_SECTOR_SIZE) {
            readFromFileAtPos(block, f, i);

            BlockData blockData = null;
            try {
                blockData = DbEncoder.decodeBlock(block, aes256Key, aes256IvMask);
            } catch (ChecksumException e) {
                LOGGER.trace("Block checksum failed", e);
            } catch (Exception e) {
                LOGGER.error("Block decoding issue", e);
            }
            if (blockData != null) {
                Block newBlock = fromBlockData(blockData);

                int newBlockId = newBlock.getBlockId();
                long newBlockVersion = newBlock.getVersion();

                lastBlockId = Math.max(lastBlockId, newBlockId);
                if (lastBlockVersion < newBlockVersion) {
                    lastBlockVersion = newBlockVersion;
                    lastBlockNumber = i / FLASH_SECTOR_SIZE;
                }

                BlockNumberAndVersion old = blockNumberAndVersionByBlockId.get(newBlockId);
                if (old == null || old.version < newBlockVersion) {
                    int blockNumber = i / FLASH_SECTOR_SIZE;

                    //Ignore tombstoned phrase blocks
                    if (newBlock.blockType() == BlockType.PHRASE_BLOCK && checkNotNull(newBlock.phraseBlock()).isTombstone()) {
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

        if (localFoldersBlockId == null) { throw new RuntimeException("Failed to find FoldersBlock"); }
        if (localPhraseTemplatesBlockId == null) { throw new RuntimeException("Failed to find PhraseTemplatesBlock"); }
        if (localSymbolSetsBlockId == null) { throw new RuntimeException("Failed to find SymbolSetsBlock"); }

        foldersBlockId = localFoldersBlockId;
        phraseTemplatesBlockId = localPhraseTemplatesBlockId;
        symbolSetsBlockId = localSymbolSetsBlockId;

        // 3. Fill phrases and phrasesByFolder
        phrases = new HashMap<>();
        phrasesByFolder = new HashMap<>();
        phraseFolders.forEach(
            (phraseIdAndName, phraseFolderAndName)
                -> {
                    phrasesByFolder
                            .computeIfAbsent(phraseFolderAndName.folderId, k -> new HashSet<>())
                            .add(phraseFolderAndName.phraseBlockId);
                    phrases.put(phraseFolderAndName.phraseBlockId, phraseFolderAndName);
            });

        // 4. Fill occupied blocks
        for (BlockNumberAndVersion bnv : blockNumberAndVersionByBlockId.values()) {
            occupiedBlocksNumbers.put(bnv.blockNumber, bnv.blockNumber);
        }

        // 5. Fill metadata caches
        // 5.1 SymbolSets Cache
        symbolSets = new HashMap<>();
        int symbolSetsBlockNumber = checkNotNull(blockNumberAndVersionByBlockId.get(symbolSetsBlockId)).blockNumber;
        loadSymbolSetsBlock(symbolSetsBlockNumber);

        // 5.2 Folders cache
        folders = new HashMap<>();
        subFoldersByFolder = new HashMap<>();
        int foldersBlockNumber = checkNotNull(blockNumberAndVersionByBlockId.get(foldersBlockId)).blockNumber;
        loadFoldersBlock(foldersBlockNumber);

        // 5.3 PhraseTemplates Cache
        phraseTemplates = new HashMap<>();
        wordTemplates = new HashMap<>();
        int phraseTemplatesBlockNumber = checkNotNull(blockNumberAndVersionByBlockId.get(phraseTemplatesBlockId)).blockNumber;
        loadPhraseTemplatesBlock(phraseTemplatesBlockNumber);
    }

    public String getDbName() {
        return dbName;
    }

    public FolderContent getFolderContent(int folderId) {
        Set<Integer> subFolderIds = subFoldersByFolder.get(folderId);
        List<Folder> subFolders = subFolderIds == null ? List.of() :
                subFolderIds.stream()
                    .map(fid -> checkNotNull(folders.get(fid)))
                    .toList();
        Set<Integer> phraseIds = phrasesByFolder.get(folderId);
        List<PhraseFolderAndName> phraseList = phraseIds == null ? List.of() :
                phraseIds.stream()
                    .map(pid -> checkNotNull(phrases.get(pid)))
                    .toList();
        return new FolderContent(subFolders, phraseList);
    }

    public @Nullable Folder getFolder(int folderId) {
        return folders.get(folderId);
    }

    // -------------------------------------------------------------------------------------

    public @Nullable PhraseBlock getPhrase(int phraseBlockId) throws IOException {
        BlockNumberAndVersion blockNumber = blockNumberAndVersionByBlockId.get(phraseBlockId);
        if (blockNumber == null) { return null; }

        byte[] block = new byte[FLASH_SECTOR_SIZE];
        int position = blockNumber.blockNumber * FLASH_SECTOR_SIZE;

        readFromFileAtPos(block, f, position);

        BlockData blockData = DbEncoder.decodeBlock(block, aes256Key, aes256IvMask);
        return FlatBufBlockDecoder.fromFlatBufPhraseBlock(blockData.blockData);
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

    // -------------------------------------------------------------------------------------
    // BlockLoaders (startup)

    // Symbol Sets
    protected void loadSymbolSetsBlock(int symbolSetsBlockNumber) throws IOException {
        Block symbolSetsBlock = readSymbolSetsBlock(symbolSetsBlockNumber);
        refreshSymbolSetsCache(symbolSetsBlock);
    }

    protected void refreshSymbolSetsCache(Block symbolSetsBlock) {
        // Fully reload SymbolSets cache
        symbolSets.clear();
        checkNotNull(symbolSetsBlock.symbolSetsBlock()).symbolSets()
                .forEach(ss -> symbolSets.put(ss.symbolSetId(), ss));
    }

    protected Block readSymbolSetsBlock(int symbolSetsBlockNumber) throws IOException {
        byte[] block = new byte[FLASH_SECTOR_SIZE];

        int symbolSetsBlockPosition = symbolSetsBlockNumber * FLASH_SECTOR_SIZE;
        readFromFileAtPos(block, f, symbolSetsBlockPosition);
        BlockData blockData = DbEncoder.decodeBlock(block, aes256Key, aes256IvMask);
        Block symbolSetsBlock = Block.of(FlatBufBlockDecoder.fromFlatBufSymbolSetsBlock(blockData.blockData));
        assert (symbolSetsBlock.blockType() == BlockType.SYMBOL_SETS_BLOCK);

        return symbolSetsBlock;
    }

    public Block readSymbolSetsBlock() throws IOException {
        int symbolSetsBlockNumber = checkNotNull(blockNumberAndVersionByBlockId.get(symbolSetsBlockId)).blockNumber;
        return readSymbolSetsBlock(symbolSetsBlockNumber);
    }

    //Phrase Templates
    protected void loadPhraseTemplatesBlock(int phraseTemplatesBlockNumber) throws IOException {
        Block phraseTemplatesBlock = readPhraseTemplatesBlock(phraseTemplatesBlockNumber);
        refreshPhraseTemplatesCache(phraseTemplatesBlock);
    }

    protected void refreshPhraseTemplatesCache(Block phraseTemplatesBlock) {
        // Fully reload PhraseTemplates cache
        phraseTemplates.clear();
        checkNotNull(phraseTemplatesBlock.phraseTemplatesBlock()).phraseTemplates()
                .forEach(pt -> phraseTemplates.put(pt.phraseTemplateId(), pt));
        wordTemplates.clear();
        checkNotNull(phraseTemplatesBlock.phraseTemplatesBlock()).wordTemplates()
                .forEach(wt -> wordTemplates.put(wt.wordTemplateId(), wt));
    }

    protected Block readPhraseTemplatesBlock(int phraseTemplatesBlockNumber) throws IOException {
        byte[] block = new byte[FLASH_SECTOR_SIZE];

        int phraseTemplatesBlockPosition = phraseTemplatesBlockNumber * FLASH_SECTOR_SIZE;
        readFromFileAtPos(block, f, phraseTemplatesBlockPosition);
        BlockData blockData = DbEncoder.decodeBlock(block, aes256Key, aes256IvMask);
        Block phraseTemplatesBlock = Block.of(FlatBufBlockDecoder.fromFlatBufPhraseTemplatesBlock(blockData.blockData));
        assert (phraseTemplatesBlock.blockType() == BlockType.PHRASE_TEMPLATES_BLOCK);
        return phraseTemplatesBlock;
    }

    public Block readPhraseTemplatesBlock() throws IOException {
        int phraseTemplatesBlock = checkNotNull(blockNumberAndVersionByBlockId.get(phraseTemplatesBlockId)).blockNumber;
        return readPhraseTemplatesBlock(phraseTemplatesBlock);
    }

    // Folders
    public void loadFoldersBlock(int foldersBlockNumber) throws IOException {
        byte[] block = new byte[FLASH_SECTOR_SIZE];

        int foldersBlockPosition = foldersBlockNumber * FLASH_SECTOR_SIZE;
        readFromFileAtPos(block, f, foldersBlockPosition);
        BlockData blockData = DbEncoder.decodeBlock(block, aes256Key, aes256IvMask);
        Block foldersBlock = Block.of(FlatBufBlockDecoder.fromFlatBufFoldersBlock(blockData.blockData));
        assert (foldersBlock.blockType() == BlockType.FOLDERS_BLOCK);

        // Fully reload Folders cache
        folders.clear();
        subFoldersByFolder.clear();
        checkNotNull(foldersBlock.foldersBlock()).folders()
                .forEach(f -> {
                    folders.put(f.folderId(), f);
                    subFoldersByFolder.computeIfAbsent(f.parentFolderId(), k -> new HashSet<>())
                            .add(f.folderId());
                });
    }

    // --------------------------------------------------------------------------------------------------------

    public void reloadBlockCache(Block block) {
        switch (block.blockType()) {
            case SYMBOL_SETS_BLOCK:
                refreshSymbolSetsCache(block);
                break;
            case KEY_BLOCK:
                // No-op, Key Block is immutable in Client Mode
                break;
            case FOLDERS_BLOCK:
                // TODO: update FOLDERS_BLOCK cache
                // - FoldersBlock cache
                //final Map<Integer, Folder> folders;
                //final Map<Integer, Set<Integer>> subFoldersByFolder;
                break;
            case PHRASE_TEMPLATES_BLOCK:
                refreshPhraseTemplatesCache(block);
                break;
            case PHRASE_BLOCK:
                // TODO: update PHRASE_BLOCK cache
                // - Phrase Blocks (minimal info) cache
                //final Map<Integer, PhraseFolderAndName> phrases;
                //final Map<Integer, Set<Integer>> phrasesByFolder;
                break;
            default:
                throw new RuntimeException("Unexpected block type " + block.blockType());
        }
    }

    //-------------------------------------------------------------------------------------------------------------

    protected int incrementAndGetBlockId() { return ++lastBlockId; }
    protected long incrementAndGetVersion() { return ++lastBlockVersion; }

    public void updateBlock(Block mainBlock) {
        BlockNumberAndVersion previousBlockInfo = blockNumberAndVersionByBlockId.get(mainBlock.getBlockId());
        int blockNumber = checkNotNull(previousBlockInfo).blockNumber;

        // 1. Find next occupied block "to the right" from the last block and move to the left
        // TODO: sometimes the 'block "to the right" from the last block' is the last recorded version of mainBlock
        //  (which is still actual, since mainBlock didn't update yet). This situation will not cause bugs, but a
        //  previous version of the same mainBlock will be moved, instead of an actual version of some other block.
        //  And the whole point of this approach is to move 2 different blocks at the same time to prevent bit rot on
        //  blocks that are rarely updated.
        //  MB put a small fix in place to guarantee that we always move a block that's not mainBlock here?
        try {
            Integer freeBlockNumber = TreeUtil.getNextMissingNumberToTheLeft(lastBlockNumber, occupiedBlocksNumbers, blockCount);
            // Make sure we have capacity to move blocks
            if (freeBlockNumber != null) {
                Integer moveBlockNumber = TreeUtil.getNextNumberToTheRight(lastBlockNumber, occupiedBlocksNumbers);

                // If found (pretty much always), move the valid block to the left, bumping the version
                if (moveBlockNumber != null) {
                    // load block at moveBlockNumber
                    Block compBlock = loadBlock(moveBlockNumber);

                    // Here we just bump the version and keep the entropy unchanged; entropy update comes with the main block
                    compBlock = nextVersion(compBlock);
                    // save the block to freeBlockNumber position
                    saveBlock(compBlock, freeBlockNumber);

                    //Update DbRuntime context:
                    reloadBlockCache(compBlock);
                    occupiedBlocksNumbers.remove(moveBlockNumber);
                    occupiedBlocksNumbers.put(freeBlockNumber, freeBlockNumber);
                    blockNumberAndVersionByBlockId.put(compBlock.getBlockId(),
                            new BlockNumberAndVersion(freeBlockNumber, compBlock.getVersion()));
                    // We're moving this backwards, so we're not updating `lastBlockNumber`, which will be updated by the main block write
                }
            }
        } catch (ChecksumException e) {
            LOGGER.trace("1st (complementary) block save: Block checksum failed", e);
        } catch (Exception e) {
            LOGGER.error("1st (complementary) block save issue", e);
        }

        // 2. Update the main block version and write it to the right of last block
        try {
            // We expect that entropy was updated by the updater of the main block
            // TODO: update entropy here?
            mainBlock = nextVersion(mainBlock);

            Integer freeBlockNumber = TreeUtil.getNextMissingNumberToTheRight(lastBlockNumber, occupiedBlocksNumbers, blockCount);
            // If we don't have capacity to move blocks, update in place
            if (freeBlockNumber == null) { freeBlockNumber = blockNumber; }

            // save to freeBlockNumber position
            saveBlock(mainBlock, freeBlockNumber);

            // Update DbRuntime context:
            reloadBlockCache(mainBlock);
            occupiedBlocksNumbers.remove(blockNumber);
            occupiedBlocksNumbers.put(freeBlockNumber, freeBlockNumber);
            blockNumberAndVersionByBlockId.put(mainBlock.getBlockId(),
                    new BlockNumberAndVersion(freeBlockNumber, mainBlock.getVersion()));
            lastBlockNumber = freeBlockNumber;
        } catch (ChecksumException e) {
            LOGGER.trace("2st (main) block save: Block checksum failed", e);
        } catch (Exception e) {
            LOGGER.error("2st (main) block save issue", e);
        }
    }

    private Block nextVersion(Block block) {
        switch (block.blockType()) {
            case KEY_BLOCK:
                return Block.of(ImmutableKeyBlock.builder()
                        .from(checkNotNull(block.keyBlock()))
                        .version(incrementAndGetVersion())
                        .build());
            case SYMBOL_SETS_BLOCK:
                return Block.of(ImmutableSymbolSetsBlock.builder()
                        .from(checkNotNull(block.symbolSetsBlock()))
                        .version(incrementAndGetVersion())
                        .build());
            case FOLDERS_BLOCK:
                return Block.of(ImmutableFoldersBlock.builder()
                        .from(checkNotNull(block.foldersBlock()))
                        .version(incrementAndGetVersion())
                        .build());
            case PHRASE_TEMPLATES_BLOCK:
                return Block.of(ImmutablePhraseTemplatesBlock.builder()
                        .from(checkNotNull(block.phraseTemplatesBlock()))
                        .version(incrementAndGetVersion())
                        .build());
            case PHRASE_BLOCK:
                return Block.of(ImmutablePhraseBlock.builder()
                        .from(checkNotNull(block.phraseBlock()))
                        .version(incrementAndGetVersion())
                        .build());
            default:
                throw new RuntimeException("Unexpected block type " + block.blockType());
        }
    }

    protected Block loadBlock(Integer fromBlockNumber) throws IOException {
        byte[] block = new byte[FLASH_SECTOR_SIZE];
        int fromBlockNumberPosition = fromBlockNumber * FLASH_SECTOR_SIZE;
        readFromFileAtPos(block, f, fromBlockNumberPosition);

        BlockNumberAndVersion b = checkNotNull(blockNumberAndVersionByBlockId.get(keyBlockId));
        boolean isKeyBlock = b.blockNumber == fromBlockNumber;
        byte[] aesKey = isKeyBlock ? keyBlockKey : aes256Key;
        byte[] ivMask = isKeyBlock ? HARDCODED_IV_MASK : aes256IvMask;

        BlockData blockData = DbEncoder.decodeBlock(block, aesKey, ivMask);
        return fromBlockData(blockData);
    }

    protected void saveBlock(Block block, Integer toBlockNumber) throws IOException {
        byte[] aesKey = block.blockType() == BlockType.KEY_BLOCK ? keyBlockKey : aes256Key;
        byte[] ivMask = block.blockType() == BlockType.KEY_BLOCK ? HARDCODED_IV_MASK : aes256IvMask;

        byte[] dataBytes = FlatBufBlockEncoder.toFlatBufBlock(block);
        byte[] blockBytes = DbEncoder.encodeBlock(dataBytes, block.blockType().code, aesKey, ivMask);

        int position = toBlockNumber * FLASH_SECTOR_SIZE;
        writeToFileAtPos(blockBytes, f, position);
    }

    public List<SymbolSet> getSymbolSets() {
        return symbolSets.values().stream().toList();
    }
}
