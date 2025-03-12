package com.phraser.dbcodec;

import com.phraser.db.Block;
import com.phraser.db.BlockType;
import com.phraser.db.PhraserDB;
import com.phraser.utils.PhraserUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.zip.Adler32;

import static com.phraser.db.Block.*;
import static com.phraser.utils.PhraserUtils.reverseArray;

/*
  TODO: I guess we can just keep this as an export-only thing

  /** 1 byte * /
  BlockType blockType();

  /** 16 bytes - matches AES data block size, not AES key size * /
  byte[] iv();
  /** 4 byte unsigned - Adler32 checksum * /
  long checksum();
*/
public class DbEncoder {
    static final SecureRandom SECURE_RANDOM = new SecureRandom();

    //TODO: extract `encodeFullBlock()` method
    byte[] encodeBlock(Block block, byte[] aesKey) {
        byte[] blockBytes = BlockEncoder.toFlatBufBlock(block);
        assert blockBytes.length <= DATA_BLOCK_SIZE;

        byte[] fullDataBytes = new byte[ENCRYPTED_BLOCK_SIZE_NO_ADLER];

        BlockType blockType = block.blockType();

        fullDataBytes[0] = blockType.code;
        ByteBuffer.wrap(fullDataBytes, 1,2)
                .order(ByteOrder.BIG_ENDIAN)
                .putShort(UnsignedConverter.intToShort(blockBytes.length));

        //fill data
        System.arraycopy(blockBytes, 0, fullDataBytes, 3, blockBytes.length);

        //fill randomness
        fillRandomBytes(fullDataBytes, blockBytes.length + 3, fullDataBytes.length);
        reverseArray(fullDataBytes);

        //TODO: the on-disk block structure has change since
        //calculate adler32
        Adler32 adler32 = new Adler32();
        adler32.update(fullDataBytes);
        long checksum = adler32.getValue();

        //TODO: remove unnecessary copy
        byte[] fullDataBytesWithAdler = new byte[ENCRYPTED_BLOCK_SIZE];
        System.arraycopy(fullDataBytes, 0, fullDataBytesWithAdler, 0, fullDataBytes.length);

        ByteBuffer.wrap(fullDataBytes, ENCRYPTED_BLOCK_SIZE_NO_ADLER,4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(UnsignedConverter.longToInt(checksum));

        //generate iv
        byte[] iv = PhraserUtils.generateAesIv(); //16 bytes
        byte[] encrypted = encrypt(fullDataBytesWithAdler, aesKey, iv);

        byte[] encodedBlock = new byte[FLASH_SECTOR_SIZE];
        System.arraycopy(encrypted, 0, encodedBlock, 0, encrypted.length);
        for (int i = ENCRYPTED_BLOCK_SIZE; i < FLASH_SECTOR_SIZE; i++) {
            encodedBlock[i] = iv[i-ENCRYPTED_BLOCK_SIZE];
        }

        return encodedBlock;
    }

    void encodeDb(PhraserDB phraserDB, byte[] aesKey) {
        for (Block block : phraserDB.blocks()) {
            byte[] encodedBlock = encodeBlock(block, aesKey);
        }
        //TODO: concatenate?
    }

    public static byte[] encrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            if (e instanceof RuntimeException) { throw (RuntimeException)e; }
            throw new RuntimeException(e);
        }
    }

    /**
     * @param arr array to fill
     * @param start startIndex inclusive
     * @param end endIndex exclusive
     */
    void fillRandomBytes(byte[] arr, int start, int end) {
        byte[] tmp = new byte[end - start];
        SECURE_RANDOM.nextBytes(tmp);

        System.arraycopy(tmp, 0, arr, start, tmp.length);
    }
}
