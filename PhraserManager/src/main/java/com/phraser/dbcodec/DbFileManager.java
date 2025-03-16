package com.phraser.dbcodec;

import com.phraser.db.Block;
import com.phraser.db.BlockType;
import com.phraser.db.KeyBlock;
import com.phraser.forms.PhraserDbForm;
import com.phraser.utils.Pbkdf2Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.Block.FLASH_SECTOR_SIZE;

public class DbFileManager {
    final static Logger LOGGER = LoggerFactory.getLogger(PhraserDbForm.class);

    public static final int PBKDF2_ITERATIONS = 10000; // Number of iterations
    public static final int PBKDF2_KEY_LENGTH = 256; // Key length in bits
    public static final String HARD_CODED_SALT_STR = "PhraserPasswordManager"; // Hardcoded salt string
    public static final byte[] HARD_CODED_SALT;
    public static final byte[] HARD_CODED_IV;

    static {
        try {
            byte[] bytes = HARD_CODED_SALT_STR.getBytes();
            HARD_CODED_SALT = MessageDigest.getInstance("SHA-256").digest(bytes);
            HARD_CODED_IV = MessageDigest.getInstance("MD5").digest(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeBlocksToFile(List<Block> srcBlocks, String password, File file) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        List<Block> blocks = new ArrayList<>(srcBlocks);

        // 1. Form KeyBlockKey from password using PBKDF2 and hardcoded stuff
        byte[] keyBlockKey = Pbkdf2Tool.pbkdf2(password, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH, HARD_CODED_SALT);
        assert(keyBlockKey.length == 32);

        // 2. Get latest KeyBlock
        Block latestKeyBlock = null;
        for (Block block : blocks) {
            if (block.blockType() == BlockType.KEY_BLOCK) {
                if (latestKeyBlock == null || latestKeyBlock.getVersion() < block.getVersion()) {
                    latestKeyBlock = block;
                }
            }
        }
        if (latestKeyBlock == null) {
            throw new RuntimeException("KeyBlock not found");
        }

        // 3. Get MainKey and IvMask from Latest KeyBlock
        byte[] mainKey = checkNotNull(latestKeyBlock.keyBlock()).key();
        byte[] ivMask = checkNotNull(latestKeyBlock.keyBlock()).iv();

        // 4. If we have less blocks than our capacity, complement with dummys
        int bucketCount = latestKeyBlock.keyBlock().bucketCount();
        if (blocks.size() < bucketCount) {
            for (int i = blocks.size(); i < bucketCount; i++) {
                blocks.add(Block.DUMMY);
            }
        }

        // 5. Shuffle blocks for security
        Collections.shuffle(blocks);

        // 6. Encrypt KeyBlocks with KeyBlockKey, other blocks with MainKey
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (Block block : blocks) {
                byte[] blockBytes;
                if (block == Block.DUMMY) {
                    blockBytes = DbEncoder.dummyBlock();
                } else {
                    byte[] dataBytes = FlatBufBlockEncoder.toFlatBufBlock(block);
                    if (block.blockType() == BlockType.KEY_BLOCK) {
                        blockBytes = DbEncoder.encodeBlock(dataBytes, block.blockType().code, keyBlockKey, HARD_CODED_IV);
                    } else {
                        blockBytes = DbEncoder.encodeBlock(dataBytes, block.blockType().code, mainKey, ivMask);
                    }
                }

                fos.write(blockBytes);
            }
        }
    }

    public static List<Block> loadBlocksFromFile(String password, File file) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        List<Block> blocks = new ArrayList<>();

        // 1. Form KeyBlockKey from password using PBKDF2 and hardcoded stuff
        byte[] keyBlockKey = Pbkdf2Tool.pbkdf2(password, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH, HARD_CODED_SALT);
        assert(keyBlockKey.length == 32);

        // 2. Locate latest KeyBlock, decrypt with KeyBlockKey
        Block latestKeyBlock = null;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[FLASH_SECTOR_SIZE];
            int bytesRead;
            // Read the file in blocks
            while ((bytesRead = fis.read(buffer)) != -1) {
                // Try to decode block
                BlockData blockData = null;
                try {
                    blockData = DbEncoder.decodeBlock(buffer, keyBlockKey, HARD_CODED_IV);
                } catch (ChecksumException e) {
                    LOGGER.trace("Block checksum failed", e);
                } catch (Exception e) {
                    LOGGER.error("Block decoding issue", e);
                }
                if (blockData != null) {
                    if (blockData.blockType == BlockType.KEY_BLOCK) {
                        Block keyBlock = Block.create(FlatBufBlockDecoder.fromFlatBufKeyBlock(blockData.blockData));
                        blocks.add(keyBlock);
                        if (latestKeyBlock == null || latestKeyBlock.getVersion() < checkNotNull(keyBlock.keyBlock()).version()) {
                            latestKeyBlock = keyBlock;
                        }
                    }
                }
            }
        }
        if (latestKeyBlock == null) {
            throw new RuntimeException("Failed to decrypt KeyBlock");
        }

        // 3. Get MainKey and IvMask from Latest KeyBlock
        byte[] mainKey = checkNotNull(latestKeyBlock.keyBlock()).key();
        byte[] ivMask = checkNotNull(latestKeyBlock.keyBlock()).iv();

        // 4. Second pass to open all blocks, decrypt with MainKey and IvMask
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[FLASH_SECTOR_SIZE];
            int bytesRead;
            // Read the file in blocks
            while ((bytesRead = fis.read(buffer)) != -1) {
                // Try to decode block
                BlockData blockData = null;
                try {
                    blockData = DbEncoder.decodeBlock(buffer, mainKey, ivMask);
                } catch (ChecksumException e) {
                    LOGGER.trace("Block checksum failed", e);
                } catch (Exception e) {
                    LOGGER.error("Block decoding issue", e);
                }
                if (blockData != null) {
                    switch (blockData.blockType) {
                        case FOLDERS_BLOCK:
                            blocks.add(Block.create(FlatBufBlockDecoder.fromFlatBufFoldersBlock(blockData.blockData)));
                            break;
                        case SYMBOL_SETS_BLOCK:
                            blocks.add(Block.create(FlatBufBlockDecoder.fromFlatBufSymbolSetsBlock(blockData.blockData)));
                            break;
                        case PHRASE_TEMPLATES_BLOCK:
                            blocks.add(Block.create(FlatBufBlockDecoder.fromFlatBufPhraseTemplatesBlock(blockData.blockData)));
                            break;
                        case PHRASE_BLOCK:
                            blocks.add(Block.create(FlatBufBlockDecoder.fromFlatBufPhraseBlock(blockData.blockData)));
                            break;
                        default:
                            throw new RuntimeException("Unexpected block type " + blockData.blockType);
                    }
                }
            }
        }

        return blocks;
    }
}
