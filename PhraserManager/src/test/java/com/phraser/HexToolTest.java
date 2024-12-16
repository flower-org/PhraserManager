package com.phraser;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class HexToolTest {
    @Test
    public void test() {
        byte[] arr = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        String str = HexTool.bytesToHex(arr);
        byte[] arr2 = HexTool.hexStringToByteArray(str);

        assertTrue(Arrays.equals(arr, arr2));
    }
}
