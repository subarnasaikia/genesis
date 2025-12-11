package com.genesis.common;

import java.util.regex.Pattern;

/**
 * Utility class for text-related operations.
 */
public final class TextUtils {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private TextUtils() {
        // Prevent instantiation
    }

    /**
     * Normalizes whitespace in text by replacing multiple whitespace characters
     * with a single space.
     *
     * @param text The input text to normalize
     * @return Normalized text
     */
    public static String normalizeWhitespace(String text) {
        return text == null ? "" : WHITESPACE_PATTERN.matcher(text.trim()).replaceAll(" ");
    }
}