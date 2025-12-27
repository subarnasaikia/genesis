package com.genesis.importexport.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Sentence segmenter based on line breaks and punctuation.
 *
 * <p>
 * Segmentation strategy:
 * <ul>
 * <li>Line breaks (newlines) always indicate sentence boundaries</li>
 * <li>Sentence-ending punctuation: period (.), danda (।), question mark (?),
 * exclamation (!)</li>
 * <li>Empty lines are skipped</li>
 * </ul>
 *
 * <p>
 * Supports Indian languages with danda (।) as sentence terminator.
 */
@Component
public class LineSentenceSegmenter implements SentenceSegmenter {

    /**
     * Pattern for sentence-ending punctuation.
     * Matches: . ? ! । ॥ (followed by optional space or end)
     */
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile(
            "([.?!।॥])(?:\\s|$)");

    /**
     * Supported language codes.
     */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "as", "bn", "hi", "brx", "en", "mr", "ne", "or", "pa", "ta", "te", "ml", "kn", "gu");

    @Override
    public List<SentenceResult> segment(String text) {
        List<SentenceResult> sentences = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return sentences;
        }

        // First, split by line breaks
        String[] lines = text.split("\\r?\\n");
        int currentOffset = 0;

        for (String line : lines) {
            // Skip empty lines
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                // Account for the newline character
                currentOffset += line.length() + 1; // +1 for newline
                continue;
            }

            // Find the actual start position (accounting for leading whitespace)
            int lineStartInOriginal = text.indexOf(line, currentOffset);
            if (lineStartInOriginal == -1) {
                lineStartInOriginal = currentOffset;
            }

            // Segment this line by sentence-ending punctuation
            List<SentenceResult> lineSentences = segmentLine(line, lineStartInOriginal);
            sentences.addAll(lineSentences);

            // Move to next line
            currentOffset = lineStartInOriginal + line.length() + 1; // +1 for newline
        }

        return sentences;
    }

    /**
     * Segment a single line into sentences based on punctuation.
     */
    private List<SentenceResult> segmentLine(String line, int lineStartOffset) {
        List<SentenceResult> sentences = new ArrayList<>();

        if (line.trim().isEmpty()) {
            return sentences;
        }

        Matcher matcher = SENTENCE_END_PATTERN.matcher(line);
        int lastEnd = 0;

        while (matcher.find()) {
            int sentenceEnd = matcher.end();
            String sentenceText = line.substring(lastEnd, sentenceEnd).trim();

            if (!sentenceText.isEmpty()) {
                // Find exact position of sentence start (skip leading whitespace)
                int leadingSpaces = 0;
                while (lastEnd + leadingSpaces < line.length() &&
                        Character.isWhitespace(line.charAt(lastEnd + leadingSpaces))) {
                    leadingSpaces++;
                }

                int startOffset = lineStartOffset + lastEnd + leadingSpaces;
                int endOffset = lineStartOffset + sentenceEnd;

                // Trim trailing whitespace from end offset
                while (endOffset > startOffset &&
                        Character.isWhitespace(line.charAt(endOffset - lineStartOffset - 1))) {
                    endOffset--;
                }

                sentences.add(new SentenceResult(sentenceText, startOffset, endOffset));
            }

            lastEnd = sentenceEnd;
        }

        // Handle remaining text (no sentence-ending punctuation)
        if (lastEnd < line.length()) {
            String remaining = line.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                int leadingSpaces = 0;
                while (lastEnd + leadingSpaces < line.length() &&
                        Character.isWhitespace(line.charAt(lastEnd + leadingSpaces))) {
                    leadingSpaces++;
                }

                int startOffset = lineStartOffset + lastEnd + leadingSpaces;
                int endOffset = lineStartOffset + line.length();

                sentences.add(new SentenceResult(remaining, startOffset, endOffset));
            }
        }

        // If no sentences found, treat entire line as one sentence
        if (sentences.isEmpty() && !line.trim().isEmpty()) {
            String trimmed = line.trim();
            int leadingSpaces = line.indexOf(trimmed.charAt(0));
            sentences.add(new SentenceResult(
                    trimmed,
                    lineStartOffset + leadingSpaces,
                    lineStartOffset + leadingSpaces + trimmed.length()));
        }

        return sentences;
    }

    @Override
    public boolean supportsLanguage(String languageCode) {
        if (languageCode == null) {
            return true;
        }
        return SUPPORTED_LANGUAGES.contains(languageCode.toLowerCase());
    }
}
