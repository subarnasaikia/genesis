package com.genesis.importexport.tokenizer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for LineSentenceSegmenter.
 */
class LineSentenceSegmenterTest {

    private LineSentenceSegmenter segmenter;

    @BeforeEach
    void setUp() {
        segmenter = new LineSentenceSegmenter();
    }

    @Test
    @DisplayName("Should segment by period")
    void segmentByPeriod() {
        String text = "First sentence. Second sentence.";
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment(text);

        assertEquals(2, sentences.size());
        assertEquals("First sentence.", sentences.get(0).getText());
        assertEquals("Second sentence.", sentences.get(1).getText());
    }

    @Test
    @DisplayName("Should segment by Assamese danda")
    void segmentByDanda() {
        String text = "প্রথম বাক্য। দ্বিতীয় বাক্য।";
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment(text);

        assertEquals(2, sentences.size());
    }

    @Test
    @DisplayName("Should segment by newline")
    void segmentByNewline() {
        String text = "Line one\nLine two";
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment(text);

        assertEquals(2, sentences.size());
        assertEquals("Line one", sentences.get(0).getText());
        assertEquals("Line two", sentences.get(1).getText());
    }

    @Test
    @DisplayName("Should handle empty text")
    void handleEmptyText() {
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment("");
        assertTrue(sentences.isEmpty());
    }

    @Test
    @DisplayName("Should handle null text")
    void handleNullText() {
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment(null);
        assertTrue(sentences.isEmpty());
    }

    @Test
    @DisplayName("Should handle single sentence without punctuation")
    void handleSingleSentenceNoPunctuation() {
        String text = "This is a single sentence";
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment(text);

        assertEquals(1, sentences.size());
        assertEquals("This is a single sentence", sentences.get(0).getText());
    }

    @Test
    @DisplayName("Should segment by question mark")
    void segmentByQuestionMark() {
        String text = "Is this a question? Yes it is.";
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment(text);

        assertEquals(2, sentences.size());
    }

    @Test
    @DisplayName("Should segment by exclamation mark")
    void segmentByExclamation() {
        String text = "Hello! How are you?";
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment(text);

        assertEquals(2, sentences.size());
    }

    @Test
    @DisplayName("Should preserve character offsets")
    void preserveOffsets() {
        String text = "First. Second.";
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment(text);

        assertEquals(2, sentences.size());

        // First sentence offsets
        assertTrue(sentences.get(0).getStartOffset() >= 0);
        assertTrue(sentences.get(0).getEndOffset() > sentences.get(0).getStartOffset());
    }

    @Test
    @DisplayName("Should skip empty lines")
    void skipEmptyLines() {
        String text = "First line\n\n\nSecond line";
        List<SentenceSegmenter.SentenceResult> sentences = segmenter.segment(text);

        assertEquals(2, sentences.size());
        assertEquals("First line", sentences.get(0).getText());
        assertEquals("Second line", sentences.get(1).getText());
    }

    @Test
    @DisplayName("Should support Assamese language")
    void supportsAssamese() {
        assertTrue(segmenter.supportsLanguage("as"));
    }

    @Test
    @DisplayName("Should support English language")
    void supportsEnglish() {
        assertTrue(segmenter.supportsLanguage("en"));
    }
}
