package com.phraser;

import com.phraser.utils.Pbkdf2Tool;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class Pbkdf2ToolTest {
    @Test
    public void test() throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] key = Pbkdf2Tool.getPbkdf2Key("QWERTY", Pbkdf2Tool.PBKDF2_ITERATIONS);
        System.out.println(HexTool.bytesToHex(key));
    }
}
