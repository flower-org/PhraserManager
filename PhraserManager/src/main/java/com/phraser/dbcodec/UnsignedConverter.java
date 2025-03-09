package com.phraser.dbcodec;

public class UnsignedConverter {
    public static int longToInt(long l) {
        return (int)(l & 0xFFFFFFFFL);
    }

    public static long intToLong(int i) {
        return i & 0xFFFFFFFFL;
    }

    public static short intToShort(int i) {
        return (short)(i & 0xFFFF);
    }

    public static int shortToInt(short s) { return s & 0xFFFF; }
}
