package com.genesis.importexport.tokenizer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for UnicodeTokenizer.
 */
class UnicodeTokenizerTest {

    private UnicodeTokenizer tokenizer;

    @BeforeEach
    void setUp() {
        tokenizer = new UnicodeTokenizer();
    }

    @Test
    @DisplayName("Should tokenize English text correctly")
    void tokenizeEnglish() {
        String text = "Hello world!";
        List<Tokenizer.TokenResult> tokens = tokenizer.tokenize(text, 0);

        assertEquals(3, tokens.size());
        assertEquals("Hello", tokens.get(0).getText());
        assertEquals("world", tokens.get(1).getText());
        assertEquals("!", tokens.get(2).getText());
    }

    @Test
    @DisplayName("Should tokenize Assamese text correctly")
    void tokenizeAssamese() {
        String text = "মই ভাত খাওঁ।";
        List<Tokenizer.TokenResult> tokens = tokenizer.tokenize(text, 0);

        assertEquals(4, tokens.size());
        assertEquals("মই", tokens.get(0).getText());
        assertEquals("ভাত", tokens.get(1).getText());
        assertEquals("খাওঁ", tokens.get(2).getText());
        assertEquals("।", tokens.get(3).getText()); // Danda as punctuation
    }

    @Test
    @DisplayName("Should preserve character offsets")
    void preserveOffsets() {
        String text = "Hello world";
        List<Tokenizer.TokenResult> tokens = tokenizer.tokenize(text, 0);

        assertEquals(2, tokens.size());

        // "Hello" at positions 0-5
        assertEquals(0, tokens.get(0).getStartOffset());
        assertEquals(5, tokens.get(0).getEndOffset());

        // "world" at positions 6-11
        assertEquals(6, tokens.get(1).getStartOffset());
        assertEquals(11, tokens.get(1).getEndOffset());
    }

    @Test
    @DisplayName("Should handle start offset parameter")
    void handleStartOffset() {
        String text = "test";
        int startOffset = 100;
        List<Tokenizer.TokenResult> tokens = tokenizer.tokenize(text, startOffset);

        assertEquals(1, tokens.size());
        assertEquals(100, tokens.get(0).getStartOffset());
        assertEquals(104, tokens.get(0).getEndOffset());
    }

    @Test
    @DisplayName("Should tokenize with multiple punctuation")
    void tokenizeWithPunctuation() {
        String text = "Hello, world! How are you?";
        List<Tokenizer.TokenResult> tokens = tokenizer.tokenize(text, 0);

        assertEquals(8, tokens.size());
        assertEquals("Hello", tokens.get(0).getText());
        assertEquals(",", tokens.get(1).getText());
        assertEquals("world", tokens.get(2).getText());
        assertEquals("!", tokens.get(3).getText());
    }

    @Test
    @DisplayName("Should handle empty text")
    void handleEmptyText() {
        List<Tokenizer.TokenResult> tokens = tokenizer.tokenize("", 0);
        assertTrue(tokens.isEmpty());
    }

    @Test
    @DisplayName("Should handle null text")
    void handleNullText() {
        List<Tokenizer.TokenResult> tokens = tokenizer.tokenize(null, 0);
        assertTrue(tokens.isEmpty());
    }

    @Test
    @DisplayName("Should tokenize Bengali text")
    void tokenizeBengali() {
        String text = "আমি ভাত খাই।";
        List<Tokenizer.TokenResult> tokens = tokenizer.tokenize(text, 0);

        assertEquals(4, tokens.size());
        assertEquals("আমি", tokens.get(0).getText());
        assertEquals("ভাত", tokens.get(1).getText());
        assertEquals("খাই", tokens.get(2).getText());
    }

    @Test
    @DisplayName("Should tokenize Hindi text")
    void tokenizeHindi() {
        String text = "मैं खाना खाता हूँ।";
        List<Tokenizer.TokenResult> tokens = tokenizer.tokenize(text, 0);

        assertTrue(tokens.size() >= 4);
        assertEquals("मैं", tokens.get(0).getText());
    }

    @Test
    @DisplayName("Should support Assamese language")
    void supportsAssamese() {
        assertTrue(tokenizer.supportsLanguage("as"));
    }

    @Test
    @DisplayName("Should support Bengali language")
    void supportsBengali() {
        assertTrue(tokenizer.supportsLanguage("bn"));
    }

    @Test
    @DisplayName("Should support Hindi language")
    void supportsHindi() {
        assertTrue(tokenizer.supportsLanguage("hi"));
    }

    @Test
    @DisplayName("Should support Bodo language")
    void supportsBodo() {
        assertTrue(tokenizer.supportsLanguage("brx"));
    }

    @Test
    @DisplayName("Should support English language")
    void supportsEnglish() {
        assertTrue(tokenizer.supportsLanguage("en"));
    }

    @Test
    @DisplayName("Should support null language (default)")
    void supportsNullLanguage() {
        assertTrue(tokenizer.supportsLanguage(null));
    }
}
