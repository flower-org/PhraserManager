package com.phraser.dbcodec;

import com.phraser.db.BlockType;
import com.phraser.utils.PhraserUtils;
import org.junit.jupiter.api.Test;

import static com.phraser.db.Block.DATA_BLOCK_SIZE;
import static com.phraser.db.Block.FLASH_SECTOR_SIZE;
import static org.junit.jupiter.api.Assertions.*;

public class DbDecoderTest {
    private static final byte[] AES_KEY = new byte[32]; // Example AES-256 key (32 bytes)
    private static final byte[] IV_MASK = new byte[16]; // Example IV mask (16 bytes)
    private static final BlockType BLOCK_TYPE = BlockType.FOLDERS_BLOCK; // Example block type

    @Test
    public void testEncodeDecode() {
        byte[] blockData = new byte[(int)(Math.random()*DATA_BLOCK_SIZE)];
        PhraserUtils.fillRandomBytes(blockData);

        // Encode the block
        byte[] encodedBlock = DbEncoder.encodeBlock(blockData, BLOCK_TYPE.code, AES_KEY, IV_MASK);

        // Decode the block
        BlockData decodedBlockData = DbEncoder.decodeBlock(encodedBlock, AES_KEY, IV_MASK);

        // Assert that the decoded block matches the original block data
        assertArrayEquals(blockData, decodedBlockData.blockData);
        assertEquals(BLOCK_TYPE, decodedBlockData.blockType);
    }

    @Test
    public void testDecodeInvalidChecksum() {
        byte[] blockData = new byte[(int)(Math.random()*DATA_BLOCK_SIZE)];
        PhraserUtils.fillRandomBytes(blockData);

        // Encode the block
        byte[] encodedBlock = DbEncoder.encodeBlock(blockData, BLOCK_TYPE.code, AES_KEY, IV_MASK);

        // Modify the encoded block to create an invalid checksum
        encodedBlock[encodedBlock.length - 1] ^= 0x01; // Flip the last byte

        // Attempt to decode the modified block, expecting an exception
        assertThrows(ChecksumException.class, () -> DbEncoder.decodeBlock(encodedBlock, AES_KEY, IV_MASK));
    }

    @Test
    public void testDecodeInvalidBlockSize() {
        // Create an invalid encoded block (wrong size)
        byte[] invalidEncodedBlock = new byte[FLASH_SECTOR_SIZE - 1]; // Invalid size

        // Attempt to decode the invalid block, expecting an exception
        assertThrows(IllegalArgumentException.class, () -> DbEncoder.decodeBlock(invalidEncodedBlock, AES_KEY, IV_MASK));
    }
}