package com.phraser.utils;

import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TreeUtilTest {
    @Test
    public void testNextMissingNumberAfterGap() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(4, 400); // 3 is missing
        tree.put(5, 500);

        Integer result = TreeUtil.getNextMissingNumberToTheRight(2, tree, 6);
        assertEquals(Integer.valueOf(3), result);
    }

    @Test
    public void testNextMissingNumberAtEnd() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(4, 400);

        Integer result = TreeUtil.getNextMissingNumberToTheRight(4, tree, 6);
        assertEquals(Integer.valueOf(5), result);
    }

    @Test
    public void testNextMissingNumberBeforeStart() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(3, 200);
        tree.put(4, 300);
        tree.put(5, 400);

        Integer result = TreeUtil.getNextMissingNumberToTheRight(1, tree, 6);
        assertEquals(Integer.valueOf(2), result); // 1 can't be taken, so next missing one is 2
    }

    @Test
    public void testNoMissingNumber() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(0, 100);
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);

        Integer result = TreeUtil.getNextMissingNumberToTheRight(2, tree, 4);
        assertNull(result); // All numbers 0 - 4 are taken
    }

    @Test
    public void testAllNumbersPresent() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(4, 400);
        tree.put(5, 500);

        Integer result = TreeUtil.getNextMissingNumberToTheRight(5, tree, 7);
        assertEquals(Integer.valueOf(6), result); // Next number is 6
    }

    @Test
    public void testEmptyTree() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();

        Integer result = TreeUtil.getNextMissingNumberToTheRight(1, tree, 6);
        assertEquals(Integer.valueOf(2), result); // 1 can't be taken, so next missing one is 2
    }

    @Test
    public void testMaxNonInclusiveBoundary() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(5, 500); // 4 is missing

        Integer result = TreeUtil.getNextMissingNumberToTheRight(3, tree, 5);
        assertEquals(Integer.valueOf(4), result); // 4 is missing
    }

    @Test
    public void testLeftNextMissingNumberToTheLeftAfterGap() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(4, 400); // 3 is missing
        tree.put(5, 500);

        Integer result = TreeUtil.getNextMissingNumberToTheLeft(4, tree, 6);
        assertEquals(Integer.valueOf(3), result); // 3 is missing
    }

    @Test
    public void testLeftNextMissingNumberAtStart() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(4, 400);

        Integer result = TreeUtil.getNextMissingNumberToTheLeft(2, tree, 6);
        assertEquals(Integer.valueOf(1), result); // 1 is missing
    }

    @Test
    public void testLeftNextMissingNumberBeforeStart() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(3, 300);
        tree.put(4, 400);
        tree.put(5, 500);

        Integer result = TreeUtil.getNextMissingNumberToTheLeft(3, tree, 6);
        assertEquals(Integer.valueOf(2), result); // 2 is missing
    }

    @Test
    public void testLeftNoMissingNumber() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(0, 100);
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);

        Integer result = TreeUtil.getNextMissingNumberToTheLeft(2, tree, 4);
        assertNull(result); // All numbers 0 - 4 are taken
    }

    @Test
    public void testLeftAllNumbersPresent() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(0, 0);
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(4, 400);
        tree.put(5, 500);

        Integer result = TreeUtil.getNextMissingNumberToTheLeft(5, tree, 6);
        assertNull(result);
    }

    @Test
    public void testLeftEmptyTree() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();

        Integer result = TreeUtil.getNextMissingNumberToTheLeft(1, tree, 6);
        assertEquals(Integer.valueOf(0), result); // 0 is missing
    }

    @Test
    public void testLeftMaxNonInclusiveBoundary() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(5, 500); // 4 is missing

        Integer result = TreeUtil.getNextMissingNumberToTheLeft(5, tree, 6);
        assertEquals(Integer.valueOf(4), result); // 4 is missing
    }

    @Test
    public void testLeftNextMissingNumberToTheLeftWithNegativeNumbers() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(-2, 200);
        tree.put(-1, 300);
        tree.put(1, 400); // 0 is missing

        Integer result = TreeUtil.getNextMissingNumberToTheLeft(1, tree, 6);
        assertEquals(Integer.valueOf(0), result); // 0 is missing
    }

    @Test
    public void testNextNumberToTheRight() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(5, 500);

        Integer result = TreeUtil.getNextNumberToTheRight(2, tree);
        assertEquals(Integer.valueOf(3), result); // Next number is 3
    }

    @Test
    public void testNextNumberToTheRightAtEnd() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(4, 400);

        Integer result = TreeUtil.getNextNumberToTheRight(5, tree);
        assertEquals(Integer.valueOf(1), result); // Flip over to get 1
    }

    @Test
    public void testNextNumberToTheRightNoGreaterNumber() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);

        Integer result = TreeUtil.getNextNumberToTheRight(3, tree);
        assertEquals(Integer.valueOf(1), result); // Flip over to get 1
    }

    @Test
    public void testNextNumberToTheRightWithNegativeNumbers() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(-3, 100);
        tree.put(-2, 200);
        tree.put(-1, 300);
        tree.put(1, 400);

        Integer result = TreeUtil.getNextNumberToTheRight(-2, tree);
        assertEquals(Integer.valueOf(-1), result); // Next number is -1
    }

    @Test
    public void testNextNumberToTheRightWithNoKeys() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();

        Integer result = TreeUtil.getNextNumberToTheRight(1, tree);
        assertNull(result); // No keys in the tree
    }

    @Test
    public void testNextNumberToTheRightEqualToKey() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);

        Integer result = TreeUtil.getNextNumberToTheRight(2, tree);
        assertEquals(Integer.valueOf(3), result); // Next number is 3
    }

    @Test
    public void testNextNumberToTheRightWithOnlyOneKey() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);

        Integer result = TreeUtil.getNextNumberToTheRight(1, tree);
        assertNull(result); // No number greater than 1
    }

    @Test
    public void testNextNumberToTheRightWithNegativeBoundary() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(-1, 100);
        tree.put(0, 200);
        tree.put(1, 300);

        Integer result = TreeUtil.getNextNumberToTheRight(-1, tree);
        assertEquals(Integer.valueOf(0), result); // Next number is 0
    }

    @Test
    public void testNextNumberToTheLeft() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(5, 500);

        Integer result = TreeUtil.getNextNumberToTheLeft(3, tree);
        assertEquals(Integer.valueOf(2), result); // Next number is 2
    }

    @Test
    public void testNextNumberToTheLeftAtStart() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(4, 400);

        Integer result = TreeUtil.getNextNumberToTheLeft(1, tree);
        assertEquals(Integer.valueOf(4), result); // Flip over to get 4
    }

    @Test
    public void testNextNumberToTheLeftNoLessNumber() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);

        Integer result = TreeUtil.getNextNumberToTheLeft(1, tree);
        assertEquals(Integer.valueOf(3), result); // Flip over to get 3
    }

    @Test
    public void testNextNumberToTheLeftWithNegativeNumbers() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(-3, 100);
        tree.put(-2, 200);
        tree.put(-1, 300);
        tree.put(1, 400);

        Integer result = TreeUtil.getNextNumberToTheLeft(-1, tree);
        assertEquals(Integer.valueOf(-2), result); // Next number is -2
    }

    @Test
    public void testNextNumberToTheLeftWithNoKeys() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();

        Integer result = TreeUtil.getNextNumberToTheLeft(1, tree);
        assertNull(result); // No keys in the tree
    }

    @Test
    public void testNextNumberToTheLeftEqualToKey() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);

        Integer result = TreeUtil.getNextNumberToTheLeft(2, tree);
        assertEquals(Integer.valueOf(1), result); // Next number is 1
    }

    @Test
    public void testNextNumberToTheLeftWithOnlyOneKey() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);

        Integer result = TreeUtil.getNextNumberToTheLeft(1, tree);
        assertNull(result); // No number less than 1
    }

    @Test
    public void testNextNumberToTheLeftWithNegativeBoundary() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(-1, 100);
        tree.put(0, 200);
        tree.put(1, 300);

        Integer result = TreeUtil.getNextNumberToTheLeft(0, tree);
        assertEquals(Integer.valueOf(-1), result); // Next number is -1
    }

    @Test
    public void testNextNumberToTheLeftWithMaxInteger() {
        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.put(1, 100);
        tree.put(2, 200);
        tree.put(3, 300);
        tree.put(4, 400);

        Integer result = TreeUtil.getNextNumberToTheLeft(Integer.MAX_VALUE, tree);
        assertEquals(Integer.valueOf(4), result); // Next number is 4
    }
}
