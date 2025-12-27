package com.genesis.importexport.tokenizer;

import java.util.List;

/**
 * Interface for sentence segmentation.
 *
 * <p>
 * Implementations split text into sentences with character offset tracking.
 */
public interface SentenceSegmenter {

    /**
     * Segment text into sentences.
     *
     * @param text the full document text
     * @return list of sentence results with positions
     */
    List<SentenceResult> segment(String text);

    /**
     * Check if this segmenter supports a specific language.
     *
     * @param languageCode ISO 639-1 language code (e.g., "as" for Assamese, "en"
     *                     for English)
     * @return true if supported
     */
    boolean supportsLanguage(String languageCode);

    /**
     * Result of segmenting a single sentence.
     */
    class SentenceResult {
        private final String text;
        private final int startOffset;
        private final int endOffset;

        public SentenceResult(String text, int startOffset, int endOffset) {
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
            return "SentenceResult{start=" + startOffset + ", end=" + endOffset +
                    ", text='" + (text.length() > 50 ? text.substring(0, 50) + "..." : text) + "'}";
        }
    }
}
