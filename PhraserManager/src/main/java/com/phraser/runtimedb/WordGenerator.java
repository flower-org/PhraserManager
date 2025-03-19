package com.phraser.runtimedb;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom(); // Single instance

    public static String generateWord(List<char[]> symbolSets, int minLength, int maxLength) {
        Set<Character> symbolSet = new HashSet<>();
        for (char[] symbolArray : symbolSets) {
            for (char c : symbolArray) {
                symbolSet.add(c);
            }
        }
        List<Character> symbols = symbolSet.stream().toList();

        // Generate a random length between minLength and maxLength
        int length = SECURE_RANDOM.nextInt(maxLength - minLength + 1) + minLength;

        StringBuilder randomString = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // Randomly select a character from the list
            char randomChar = symbols.get(SECURE_RANDOM.nextInt(symbols.size()));
            randomString.append(randomChar);
        }

        return randomString.toString();
    }
}
