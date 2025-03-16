package com.phraser;

import com.phraser.utils.PhraserUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArrayUtilTest {
    @Test
    public void testReverseArray_ValidInput() {
        byte[] arr = {1, 2, 3, 4, 5};
        PhraserUtils.reverseArrayInPlace(arr, 1, 3); // Reverse elements at indices 1 to 3
        assertArrayEquals(new byte[]{1, 3, 2, 4, 5}, arr);
    }

    @Test
    public void testReverseArray_almostFullArray() {
        byte[] arr = {1, 2, 3, 4, 5};
        PhraserUtils.reverseArrayInPlace(arr, 0, 4); // Reverse the entire array
        assertArrayEquals(new byte[]{4, 3, 2, 1, 5}, arr);
    }

    @Test
    public void testReverseArray_FullArray() {
        byte[] arr = {1, 2, 3, 4, 5};
        PhraserUtils.reverseArrayInPlace(arr, 0, 5); // Reverse the entire array
        assertArrayEquals(new byte[]{5, 4, 3, 2, 1}, arr);
    }

    @Test
    public void testReverseArray_SingleElement() {
        byte[] arr = {1, 2, 3, 4, 5};
        PhraserUtils.reverseArrayInPlace(arr, 2, 2); // Reverse a single element
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, arr); // No change expected
    }

    @Test
    public void testReverseArray_EmptyArray() {
        byte[] arr = {};
        PhraserUtils.reverseArrayInPlace(arr, 0, 0); // Reverse an empty array
        assertArrayEquals(new byte[]{}, arr); // No change expected
    }

    @Test
    public void testReverseArray_InvalidStartIndex() {
        byte[] arr = {1, 2, 3, 4, 5};
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> {
            PhraserUtils.reverseArrayInPlace(arr, -1, 3); // Invalid start index
        });
        assertEquals("Invalid start or end index", exception.getMessage());
    }

    @Test
    public void testReverseArray_InvalidEndIndex() {
        byte[] arr = {1, 2, 3, 4, 5};
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> {
            PhraserUtils.reverseArrayInPlace(arr, 1, 6); // Invalid end index
        });
        assertEquals("Invalid start or end index", exception.getMessage());
    }

    @Test
    public void testReverseArray_StartGreaterThanEnd() {
        byte[] arr = {1, 2, 3, 4, 5};
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> {
            PhraserUtils.reverseArrayInPlace(arr, 3, 1); // Start index greater than end index
        });
        assertEquals("Invalid start or end index", exception.getMessage());
    }
}
