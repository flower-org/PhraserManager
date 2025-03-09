package com.phraser.dbcodec;

import com.phraser.db.BlockType;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Adler32;

import static com.phraser.db.Block.*;
import static com.phraser.db.Block.ENCRYPTED_BLOCK_SIZE;
import static com.phraser.utils.PhraserUtils.reverseArray;

public class DbDecoder {
    //Block
    void decodeBlock(byte[] encodedBlock, byte[] aesKey) {
        byte[] iv = new byte[16];
        System.arraycopy(encodedBlock, ENCRYPTED_BLOCK_SIZE, iv, 0, iv.length);

        byte[] encrypted = new byte[ENCRYPTED_BLOCK_SIZE];
        System.arraycopy(encodedBlock, 0, encrypted, 0, ENCRYPTED_BLOCK_SIZE);

        byte[] fullDataBytesWithAdler = decrypt(encrypted, aesKey, iv);
        byte[] fullDataBytes = new byte[ENCRYPTED_BLOCK_SIZE_NO_ADLER];
        System.arraycopy(fullDataBytesWithAdler, 0, fullDataBytes, 0, ENCRYPTED_BLOCK_SIZE_NO_ADLER);

        long adler = UnsignedConverter.intToLong(ByteBuffer.wrap(fullDataBytesWithAdler, ENCRYPTED_BLOCK_SIZE_NO_ADLER, 4)
                                .order(ByteOrder.BIG_ENDIAN)
                                .getInt());

        Adler32 adler32 = new Adler32();
        adler32.update(fullDataBytes);
        long checksum = adler32.getValue();

        assert(checksum == adler);

        reverseArray(fullDataBytes);

        BlockType blockType = BlockType.fromCode(fullDataBytes[0]);
        int dataLength = UnsignedConverter.shortToInt(ByteBuffer.wrap(fullDataBytes, 1,2)
                .order(ByteOrder.BIG_ENDIAN)
                .getShort());
        byte[] blockBytes = new byte[dataLength];
        System.arraycopy(fullDataBytes, 3, blockBytes, 0, dataLength);

//        TODO: decode blck here
    }

    byte[] decrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            return cipher.doFinal(data);
        } catch(Exception e) {
            if (e instanceof RuntimeException) { throw (RuntimeException)e; }
            throw new RuntimeException(e);
        }
    }
}
