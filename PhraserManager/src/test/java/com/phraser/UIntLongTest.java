package com.phraser;

import com.phraser.dbcodec.UnsignedConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UIntLongTest {
    @Test
    public void test() {
        int i = 500;
        long l = UnsignedConverter.intToLong(i);
        assertEquals(l, 500L);
        int i2 = UnsignedConverter.longToInt(l);
        assertEquals(i, i2);
    }

    @Test
    public void test2() {
        long l = Integer.MAX_VALUE;
        l = l + 1;
        int i = UnsignedConverter.longToInt(l);
        long l2 = UnsignedConverter.intToLong(i);
        assertEquals(l, l2);
    }

    @Test
    public void test3() {
        int i = -500;
        long l = UnsignedConverter.intToLong(i);
        int i2 = UnsignedConverter.longToInt(l);
        assertEquals(i, i2);
    }

    @Test
    public void test1() {
        short s = 500;
        int i = UnsignedConverter.shortToInt(s);
        assertEquals(i, 500L);
        short s2 = UnsignedConverter.intToShort(i);
        assertEquals(s, s2);
    }

    @Test
    public void test12() {
        int i = Short.MAX_VALUE;
        i = i + 1;
        short s = UnsignedConverter.intToShort(i);
        int i2 = UnsignedConverter.shortToInt(s);
        assertEquals(i, i2);
    }

    @Test
    public void test13() {
        short s = -500;
        int i = UnsignedConverter.shortToInt(s);
        short s2 = UnsignedConverter.intToShort(i);
        assertEquals(s, s2);
    }
}
