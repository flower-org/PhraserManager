package com.phraser.utils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PhraserUtils {
    public static final byte GENERATEABLE = 1;
    public static final byte TYPEABLE = 2;
    public static final byte VIEWABLE = 4;
    public static final byte USER_EDITABLE = 8;

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

    public static byte getWordPermissions(boolean isGenerateable, boolean isUserEditable,
                                   boolean isTypeable, boolean isViewable) {
        if (!isGenerateable && !isUserEditable) {
            throw new RuntimeException("Word should be either Generateable or UserEditable or both");
        }
        if (!isTypeable && !isViewable) {
            throw new RuntimeException("Word should be either Typeable or Viewable or both");
        }

        int getWordPermissions = 0;
        if (isGenerateable) {
            getWordPermissions = getWordPermissions | GENERATEABLE;
        }
        if (isTypeable) {
            getWordPermissions = getWordPermissions | TYPEABLE;
        }
        if (isViewable) {
            getWordPermissions = getWordPermissions | VIEWABLE;
        }
        if (isUserEditable) {
            getWordPermissions = getWordPermissions | USER_EDITABLE;
        }

        return (byte)getWordPermissions;
    }

    public static boolean isGenerateable(byte permissions) { return (permissions & GENERATEABLE) == GENERATEABLE; }
    public static boolean isUserEditable(byte permissions) { return (permissions & USER_EDITABLE) == USER_EDITABLE; }
    public static boolean isTypeable(byte permissions) { return (permissions & TYPEABLE) == TYPEABLE; }
    public static boolean isViewable(byte permissions) { return (permissions & VIEWABLE) == VIEWABLE; }

    public static byte removeGenerateable(byte permissions) { return (byte) (permissions & ~GENERATEABLE); }
    public static byte removeUserEditable(byte permissions) { return (byte) (permissions & ~USER_EDITABLE); }
    public static byte removeTypeable(byte permissions) { return (byte) (permissions & ~TYPEABLE); }
    public static byte removeViewable(byte permissions) { return (byte) (permissions & ~VIEWABLE); }
}
