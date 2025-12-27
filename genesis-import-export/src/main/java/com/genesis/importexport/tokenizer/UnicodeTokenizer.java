package com.genesis.importexport.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Unicode-based word tokenizer supporting multiple scripts.
 *
 * <p>
 * Uses Unicode character properties to tokenize text, supporting:
 * <ul>
 * <li>Assamese (অসমীয়া)</li>
 * <li>Bengali (বাংলা)</li>
 * <li>Hindi (हिंदी)</li>
 * <li>Bodo (बड़ो)</li>
 * <li>English and other Latin-based languages</li>
 * </ul>
 *
 * <p>
 * Tokenization strategy:
 * <ul>
 * <li>Words are sequences of letters/digits</li>
 * <li>Punctuation marks are separate tokens</li>
 * <li>Whitespace is used as delimiter (not included in tokens)</li>
 * </ul>
 */
@Component
public class UnicodeTokenizer implements Tokenizer {

    /**
     * Supported language codes.
     */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "as", // Assamese
            "bn", // Bengali
            "hi", // Hindi
            "brx", // Bodo
            "en", // English
            "mr", // Marathi
            "ne", // Nepali
            "or", // Odia
            "pa", // Punjabi
            "ta", // Tamil
            "te", // Telugu
            "ml", // Malayalam
            "kn", // Kannada
            "gu" // Gujarati
    );

    @Override
    public List<TokenResult> tokenize(String text, int startOffset) {
        List<TokenResult> tokens = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return tokens;
        }

        int currentPos = 0;
        int length = text.length();

        while (currentPos < length) {
            char c = text.charAt(currentPos);

            // Skip whitespace
            if (Character.isWhitespace(c)) {
                currentPos++;
                continue;
            }

            // Check if it's a word character (letter or digit)
            if (isWordCharacter(c)) {
                int tokenStart = currentPos;

                // Collect all consecutive word characters
                while (currentPos < length && isWordCharacter(text.charAt(currentPos))) {
                    currentPos++;
                }

                String tokenText = text.substring(tokenStart, currentPos);
                tokens.add(new TokenResult(
                        tokenText,
                        startOffset + tokenStart,
                        startOffset + currentPos));
            }
            // Handle punctuation as separate tokens
            else if (isPunctuation(c)) {
                tokens.add(new TokenResult(
                        String.valueOf(c),
                        startOffset + currentPos,
                        startOffset + currentPos + 1));
                currentPos++;
            }
            // Skip unknown characters
            else {
                currentPos++;
            }
        }

        return tokens;
    }

    @Override
    public boolean supportsLanguage(String languageCode) {
        if (languageCode == null) {
            return true; // Default support
        }
        return SUPPORTED_LANGUAGES.contains(languageCode.toLowerCase());
    }

    /**
     * Check if a character is a word character (part of a token).
     * Includes letters, digits, and combining marks.
     */
    private boolean isWordCharacter(char c) {
        int type = Character.getType(c);
        return Character.isLetterOrDigit(c) ||
                type == Character.NON_SPACING_MARK || // Combining diacritical marks
                type == Character.COMBINING_SPACING_MARK || // Spacing combining marks
                type == Character.ENCLOSING_MARK || // Enclosing marks
                isIndianScriptConnector(c); // Script-specific connectors
    }

    /**
     * Check if character is a punctuation mark.
     */
    private boolean isPunctuation(char c) {
        int type = Character.getType(c);
        return type == Character.START_PUNCTUATION ||
                type == Character.END_PUNCTUATION ||
                type == Character.CONNECTOR_PUNCTUATION ||
                type == Character.DASH_PUNCTUATION ||
                type == Character.INITIAL_QUOTE_PUNCTUATION ||
                type == Character.FINAL_QUOTE_PUNCTUATION ||
                type == Character.OTHER_PUNCTUATION ||
                isIndianPunctuation(c);
    }

    /**
     * Check for Indian script-specific connectors that should be part of words.
     * Examples: Zero-width joiner, Zero-width non-joiner
     */
    private boolean isIndianScriptConnector(char c) {
        return c == '\u200C' || // Zero-width non-joiner (ZWNJ)
                c == '\u200D'; // Zero-width joiner (ZWJ)
    }

    /**
     * Check for Indian script-specific punctuation.
     */
    private boolean isIndianPunctuation(char c) {
        return c == '\u0964' || // Devanagari/Bengali danda (full stop)
                c == '\u0965' || // Double danda
                c == '\u0970' || // Devanagari abbreviation sign
                c == '\u09F7' || // Bengali section sign
                c == '\u201C' || // Left double quotation mark
                c == '\u201D' || // Right double quotation mark
                c == '\u2018' || // Left single quotation mark
                c == '\u2019'; // Right single quotation mark
    }
}
