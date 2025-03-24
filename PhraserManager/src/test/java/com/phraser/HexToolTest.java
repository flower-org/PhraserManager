package com.phraser;

import com.phraser.utils.Pbkdf2Tool;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static com.phraser.utils.Pbkdf2Tool.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HexToolTest {
    @Test
    public void test() {
        byte[] arr = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        String str = HexTool.bytesToHex(arr);
        byte[] arr2 = HexTool.hexStringToByteArray(str);

        assertTrue(Arrays.equals(arr, arr2));
    }

    @Test
    void pbkdf2Test() throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] key = Pbkdf2Tool.pbkdf2("QWERTY", 12345, PBKDF2_KEY_LENGTH, HARDCODED_SALT);
        String str3 = HexTool.bytesToHex(key);

        System.out.println(str3);
    }
}
