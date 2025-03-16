package com.phraser.utils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PhraserUtils {
    static final SecureRandom SECURE_RANDOM = new SecureRandom();
    static final KeyGenerator KEY_GEN;
    static {
        try {
            KEY_GEN = KeyGenerator.getInstance("AES");
            KEY_GEN.init(256);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static SecretKey getAes256Key() {
        return KEY_GEN.generateKey();
    }

    public static long generateEntropy() {
        return SECURE_RANDOM.nextLong();
    }

    public static byte[] generateAesIv() {
        byte[] iv = new byte[16];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    public static void reverseArrayInPlace(byte[] arr, int start, int end) {
        if (start < 0 || end > arr.length || start > end) {
            throw new IndexOutOfBoundsException("Invalid start or end index");
        }
        int length = end-start;
        for (int i = 0; i < length/2; i++) {
            byte tmp = arr[start + i];
            arr[start + i] = arr[end - 1 - i];
            arr[end - 1 - i] = tmp;
        }
    }

    public static void fillRandomBytes(byte[] arr) {
        fillRandomBytes(arr, 0, arr.length);
    }

    /**
     * @param arr array to fill
     * @param start startIndex inclusive
     * @param end endIndex non-inclusive
     */
    public static void fillRandomBytes(byte[] arr, int start, int end) {
        byte[] tmp = new byte[end - start];
        SECURE_RANDOM.nextBytes(tmp);
//        System.out.println("random bytes " + HexTool.bytesToHex(tmp));

        System.arraycopy(tmp, 0, arr, start, tmp.length);
    }

    public static byte[] xorByteArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length];
        for (int i = 0; i < array1.length; i++) {
            result[i] = (byte) (array1[i] ^ array2[i]);
        }
        return result;
    }
}
