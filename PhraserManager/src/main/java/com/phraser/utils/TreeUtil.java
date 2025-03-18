package com.phraser.utils;

import javax.annotation.Nullable;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public class TreeUtil {
    public static @Nullable Integer getNextNumberToTheRight(int num, TreeMap<Integer, ?> tree) {
        Integer nextNumber = tree.higherKey(num);
        if (nextNumber == null) {
            // If nothing found to the right, flip over
            nextNumber = tree.higherKey(-1);
            if (Objects.equals(nextNumber, num)) { nextNumber = null; }
        }
        return nextNumber;
    }

    public static @Nullable Integer getNextNumberToTheLeft(int num, TreeMap<Integer, ?> tree) {
        Integer nextNumber = tree.lowerKey(num);
        if (nextNumber == null) {
            // If nothing found to the left, flip over
            nextNumber = tree.lowerKey(Integer.MAX_VALUE);
            if (Objects.equals(nextNumber, num)) { nextNumber = null; }
        }
        return nextNumber;
    }

    public static @Nullable Integer getNextMissingNumberToTheRight(int num, TreeMap<Integer, ?> tree, int maxNonInclusiveNumber) {
        // Seek from num to the right
        NavigableMap<Integer, ?> rightTree = tree.subMap(num, false, maxNonInclusiveNumber, false);
        int cursor = num;
        for (Integer n : rightTree.keySet()) {
            if (n != cursor+1) {
                return cursor + 1;
            }
            cursor = n;
        }

        ++cursor;
        if (cursor < maxNonInclusiveNumber) {
            return cursor;
        }

        // Flip over
        NavigableMap<Integer, ?> leftTree = tree.subMap(-1, false, num, false);
        cursor = -1;
        for (Integer n : leftTree.keySet()) {
            if (n != cursor+1) {
                return cursor + 1;
            }
            cursor = n;
        }

        ++cursor;
        if (cursor < num) {
            return cursor;
        }

        return null;
    }

    public static @Nullable Integer getNextMissingNumberToTheLeft(int num, TreeMap<Integer, ?> tree, int maxNonInclusiveNumber) {
        // Seek from num to the right
        NavigableMap<Integer, ?> leftTree = tree.subMap(-1, false, num, false);
        int cursor = num;
        for (Integer n : leftTree.descendingKeySet()) {
            if (n != cursor-1) {
                return cursor - 1;
            }
            cursor = n;
        }

        --cursor;
        if (cursor >= 0) {
            return cursor;
        }

        // Flip over
        NavigableMap<Integer, ?> rightTree = tree.subMap(num, false, maxNonInclusiveNumber, false);
        cursor = maxNonInclusiveNumber;
        for (Integer n : rightTree.descendingKeySet()) {
            if (n != cursor-1) {
                return cursor - 1;
            }
            cursor = n;
        }

        --cursor;
        if (cursor > num) {
            return cursor;
        }


        return null;
    }
}
