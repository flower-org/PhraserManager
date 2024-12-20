package com.phraser.utils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PhraserUtils {
    static final KeyGenerator KEY_GEN;
    static final SecureRandom SECURE_RANDOM;
    static {
        try {
            SECURE_RANDOM = new SecureRandom();

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
}
