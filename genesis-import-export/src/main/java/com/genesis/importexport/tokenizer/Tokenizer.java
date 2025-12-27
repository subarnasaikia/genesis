package com.genesis.importexport.tokenizer;

import java.util.List;

/**
 * Interface for word tokenization.
 *
 * <p>
 * Implementations split text into individual tokens (words, punctuation)
 * with character offset tracking.
 */
public interface Tokenizer {

    /**
     * Tokenize a sentence into individual tokens.
     *
     * @param text        the sentence text to tokenize
     * @param startOffset the character offset where this text starts in the
     *                    original document
     * @return list of token results with positions
     */
    List<TokenResult> tokenize(String text, int startOffset);

    /**
     * Check if this tokenizer supports a specific language.
     *
     * @param languageCode ISO 639-1 language code (e.g., "as" for Assamese, "en"
     *                     for English)
     * @return true if supported
     */
    boolean supportsLanguage(String languageCode);

    /**
     * Result of tokenizing a single token.
     */
    class TokenResult {
        private final String text;
        private final int startOffset;
        private final int endOffset;

        public TokenResult(String text, int startOffset, int endOffset) {
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

        @Override
        public String toString() {
            return "TokenResult{text='" + text + "', start=" + startOffset + ", end=" + endOffset + "}";
        }
    }
}
