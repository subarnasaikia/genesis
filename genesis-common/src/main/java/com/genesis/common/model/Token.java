package com.genesis.common.model;

/**
 * Represents a token in the text, with its position information.
 */
public class Token {
    private final String text;
    private final int startOffset;
    private final int endOffset;

    public Token(String text, int startOffset, int endOffset) {
        this.text = text;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public String getText() {
        return text;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }
}