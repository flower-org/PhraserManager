package com.phraser.dbcodec;

import com.phraser.db.Block;
import com.phraser.db.BlockType;
import com.phraser.utils.PhraserUtils;
import com.phraser.utils.UnsignedConverter;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.zip.Adler32;

import static com.phraser.db.Block.*;

public class DbEncoder {
    public final static String AES_ALGORITHM = "AES";
    public final static String AES_CBC_TRANSFORM = "AES/CBC/NoPadding";

    public static byte[] encodeBlock(Block block, byte[] aesKey, byte[] ivMask) {
        byte[] blockFlatBufData = FlatBufBlockEncoder.toFlatBufBlock(block);
        return encodeBlock(blockFlatBufData, block.blockType().code, aesKey, ivMask);
    }

    public static byte[] encodeBlock(byte[] blockFlatBufData, byte blockType, byte[] aesKey, byte[] ivMask) {
        assert blockFlatBufData.length <= DATA_BLOCK_SIZE;

        // Create a single array to hold the block data and checksum
        byte[] fullDataBytesWithAdler = new byte[ENCRYPTED_BLOCK_SIZE];

        // Set block type and length
        fullDataBytesWithAdler[0] = blockType;

        int length = UnsignedConverter.intToShort(blockFlatBufData.length);
        fullDataBytesWithAdler[1] = (byte) (length >> 8); // High byte
        fullDataBytesWithAdler[2] = (byte) (length & 0xFF); // Low byte

//        System.out.println("block type " + blockType);
//        System.out.println("data length " + length);

        //fill data
        System.arraycopy(blockFlatBufData, 0, fullDataBytesWithAdler, 3, blockFlatBufData.length);

        //fill randomness
        PhraserUtils.fillRandomBytes(fullDataBytesWithAdler, blockFlatBufData.length + 3, ENCRYPTED_BLOCK_SIZE_NO_ADLER);
        PhraserUtils.reverseArrayInPlace(fullDataBytesWithAdler, 0, ENCRYPTED_BLOCK_SIZE_NO_ADLER);

        //calculate adler32
        Adler32 adler32 = new Adler32();
        adler32.update(fullDataBytesWithAdler, 0, ENCRYPTED_BLOCK_SIZE_NO_ADLER);
        long checksum = adler32.getValue();
//        System.out.println("checksum " + checksum);

        // Directly assign the checksum bytes in big-endian order
        int checksumInt = UnsignedConverter.longToInt(checksum);
//        System.out.println("checksumInt " + checksumInt);
        fullDataBytesWithAdler[ENCRYPTED_BLOCK_SIZE_NO_ADLER] = (byte) (checksumInt >> 24); // High byte
        fullDataBytesWithAdler[ENCRYPTED_BLOCK_SIZE_NO_ADLER + 1] = (byte) (checksumInt >> 16); // Second byte
        fullDataBytesWithAdler[ENCRYPTED_BLOCK_SIZE_NO_ADLER + 2] = (byte) (checksumInt >> 8); // Third byte
        fullDataBytesWithAdler[ENCRYPTED_BLOCK_SIZE_NO_ADLER + 3] = (byte) (checksumInt & 0xFF); // Low byte

//        System.out.println("decrypted " + HexTool.bytesToHex(fullDataBytesWithAdler));
//        System.out.println("decrypted size " + fullDataBytesWithAdler.length);

        //generate iv and encrypt
        byte[] ivPart = PhraserUtils.generateAesIv(); //16 bytes
        byte[] iv = PhraserUtils.xorByteArrays(ivMask, ivPart);
        byte[] encrypted = aes256CbcEncrypt(fullDataBytesWithAdler, aesKey, iv);

//        System.out.println("encrypted " + HexTool.bytesToHex(encrypted));
//        System.out.println("encrypted size " + encrypted.length);

        byte[] encodedBlock = new byte[FLASH_SECTOR_SIZE];
        System.arraycopy(encrypted, 0, encodedBlock, 0, encrypted.length);
        System.arraycopy(ivPart, 0, encodedBlock, ENCRYPTED_BLOCK_SIZE, ivPart.length);

//        System.out.println("IvPart " + HexTool.bytesToHex(ivPart));
//        System.out.println("IvMask " + HexTool.bytesToHex(ivMask));
//        System.out.println("IV " + HexTool.bytesToHex(iv));

        return encodedBlock;
    }

    public static BlockData decodeBlock(byte[] encoded, byte[] aesKey, byte[] ivMask) throws ChecksumException {
        // Ensure the encoded data is of the expected size
        if (encoded.length != FLASH_SECTOR_SIZE) {
            throw new IllegalArgumentException("Invalid encoded block size, got " + encoded.length +
                    " bytes, expected " + FLASH_SECTOR_SIZE + " bytes" );
        }

        // Extract the IV part from the encoded data
        byte[] ivPart = new byte[16]; // Assuming IV is 16 bytes for AES
        System.arraycopy(encoded, ENCRYPTED_BLOCK_SIZE, ivPart, 0, ivPart.length);

        // Generate the actual IV by XORing with the ivMask
        byte[] iv = PhraserUtils.xorByteArrays(ivMask, ivPart);

//        System.out.println("IvPart " + HexTool.bytesToHex(ivPart));
//        System.out.println("IvMask " + HexTool.bytesToHex(ivMask));
//        System.out.println("IV " + HexTool.bytesToHex(iv));

        // Extract the encrypted data
        byte[] encrypted = new byte[ENCRYPTED_BLOCK_SIZE];
        System.arraycopy(encoded, 0, encrypted, 0, ENCRYPTED_BLOCK_SIZE);

//        System.out.println("encrypted " + HexTool.bytesToHex(encrypted));
//        System.out.println("encrypted size " + encrypted.length);

        // Decrypt the data
        byte[] decrypted = aes256CbcDecrypt(encrypted, aesKey, iv);

//        System.out.println("decrypted " + HexTool.bytesToHex(decrypted));
//        System.out.println("decrypted size " + decrypted.length);

        // Verify the checksum
        int checksumInt = ((decrypted[ENCRYPTED_BLOCK_SIZE_NO_ADLER] & 0xFF) << 24) |
                ((decrypted[ENCRYPTED_BLOCK_SIZE_NO_ADLER + 1] & 0xFF) << 16) |
                ((decrypted[ENCRYPTED_BLOCK_SIZE_NO_ADLER + 2] & 0xFF) << 8) |
                (decrypted[ENCRYPTED_BLOCK_SIZE_NO_ADLER + 3] & 0xFF);

//        System.out.println("decr checksumInt " + checksumInt);

        // Calculate the Adler32 checksum of the decrypted data (excluding the checksum bytes)
        Adler32 adler32 = new Adler32();
        adler32.update(decrypted, 0, ENCRYPTED_BLOCK_SIZE_NO_ADLER);
        long calculatedChecksum = adler32.getValue();
        int calculatedChecksumInt = UnsignedConverter.longToInt(calculatedChecksum);

//        System.out.println("calc checksum " + calculatedChecksum);
//        System.out.println("calc checksumInt " + calculatedChecksumInt);

        // Verify the checksum
        if (calculatedChecksumInt != checksumInt) {
            throw new ChecksumException("Checksum verification failed");
        }

        PhraserUtils.reverseArrayInPlace(decrypted, 0, ENCRYPTED_BLOCK_SIZE_NO_ADLER);

        // Extract the original block data length
        byte blockType = decrypted[0];
        int length = ((decrypted[1] & 0xFF) << 8) | (decrypted[2] & 0xFF);
        byte[] blockFlatBufData = new byte[length];

//        System.out.println("block type " + blockType);
//        System.out.println("data length " + length);

        // Copy the original block data
        System.arraycopy(decrypted, 3, blockFlatBufData, 0, length);

        return new BlockData(BlockType.fromCode(blockType), blockFlatBufData);
    }

    public static byte[] aes256CbcEncrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, AES_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(AES_CBC_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            if (e instanceof RuntimeException) { throw (RuntimeException)e; }
            throw new RuntimeException(e);
        }
    }

    public static byte[] aes256CbcDecrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, AES_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(AES_CBC_TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            return cipher.doFinal(data);
        } catch(Exception e) {
            if (e instanceof RuntimeException) { throw (RuntimeException)e; }
            throw new RuntimeException(e);
        }
    }
}
