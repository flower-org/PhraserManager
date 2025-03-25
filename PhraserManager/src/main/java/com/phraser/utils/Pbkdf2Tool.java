package com.phraser.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class Pbkdf2Tool {
    public static final int PBKDF2_ITERATIONS = 10239; // Number of iterations
    public static final int PBKDF2_KEY_LENGTH = 256; // Key length in bits

    public static final String HARDCODED_PHRASER_TOKEN = "PhraserPasswordManager"; // Hardcoded salt string
    public static final byte[] HARDCODED_SALT;
    public static final byte[] HARDCODED_IV_MASK;

    static {
        try {
            byte[] bytes = HARDCODED_PHRASER_TOKEN.getBytes();
            HARDCODED_SALT = MessageDigest.getInstance("SHA-256").digest(bytes);
            HARDCODED_IV_MASK = MessageDigest.getInstance("MD5").digest(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static byte[] getPbkdf2Key(String password, int iterations) throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] keyBlockKey = Pbkdf2Tool.pbkdf2(password, iterations, PBKDF2_KEY_LENGTH, HARDCODED_SALT);
        assert(keyBlockKey.length == 32);
        return keyBlockKey;
    }

    public static byte[] pbkdf2(String password, int iterations, int keyLength, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }
}
