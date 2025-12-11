package com.genesis.common;

/**
 * Common constants used across the application.
 */
public final class GenesisCoreConstants {
    private GenesisCoreConstants() {
        // Prevent instantiation
    }

    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    // Document related constants
    public static final String TXT_EXTENSION = ".txt";
    public static final String CONLL_EXTENSION = ".conll";
    public static final String XMI_EXTENSION = ".xmi";
}