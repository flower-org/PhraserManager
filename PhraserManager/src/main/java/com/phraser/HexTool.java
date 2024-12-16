package com.phraser;

public class HexTool {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString  = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int d1 = Character.digit(hex.charAt(i), 16);
            int d2 = Character.digit(hex.charAt(i + 1), 16);
            data[i / 2] = (byte)((d1 << 4) + (d2));
        }
        return data;
    }
}
