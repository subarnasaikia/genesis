package com.genesis.common.model;

import java.util.List;

/**
 * Represents processed document text with its structural information.
 */
public class DocumentText {
    private final String rawText;
    private final List<Token> tokens;

    public DocumentText(String rawText, List<Token> tokens) {
        this.rawText = rawText;
        this.tokens = tokens;
    }

    public String getRawText() {
        return rawText;
    }

    public List<Token> getTokens() {
        return tokens;
    }
}