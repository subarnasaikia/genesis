package com.genesis.importexport.format;

import static org.junit.jupiter.api.Assertions.*;

import com.genesis.common.exception.ValidationException;
import com.genesis.importexport.dto.ExportOptions;
import com.genesis.importexport.entity.SentenceEntity;
import com.genesis.importexport.entity.TokenEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Conll2012Parser.
 */
class Conll2012ParserTest {

    private Conll2012Parser parser;
    private UUID testDocumentId;

    @BeforeEach
    void setUp() {
        parser = new Conll2012Parser();
        testDocumentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should parse basic CoNLL-2012 file")
    void parseBasicConll() throws IOException {
        String content = """
                #begin document (test_doc); part 000
                test_doc\t0\t0\tHello\tNNP\t*\t-\t-\t-\t-\t*\t-
                test_doc\t0\t1\tworld\tNN\t*\t-\t-\t-\t-\t*\t-

                #end document
                """;

        Conll2012Parser.ParseResult result = parser.parse(content, testDocumentId);

        assertEquals("test_doc", result.getDocumentId());
        assertEquals(1, result.getSentences().size());
        assertEquals(2, result.getTokens().size());
        assertEquals("Hello", result.getTokens().get(0).getForm());
        assertEquals("world", result.getTokens().get(1).getForm());
    }

    @Test
    @DisplayName("Should parse multiple sentences")
    void parseMultipleSentences() throws IOException {
        String content = """
                #begin document (test_doc); part 000
                test_doc\t0\t0\tFirst\tNN\t*\t-\t-\t-\t-\t*\t-
                test_doc\t0\t1\t.\t.\t*\t-\t-\t-\t-\t*\t-

                test_doc\t0\t0\tSecond\tNN\t*\t-\t-\t-\t-\t*\t-
                test_doc\t0\t1\t.\t.\t*\t-\t-\t-\t-\t*\t-

                #end document
                """;

        Conll2012Parser.ParseResult result = parser.parse(content, testDocumentId);

        assertEquals(2, result.getSentences().size());
        assertEquals(4, result.getTokens().size());
    }

    @Test
    @DisplayName("Should parse single-token coreference")
    void parseSingleTokenCoreference() throws IOException {
        String content = """
                #begin document (test_doc); part 000
                test_doc\t0\t0\tJohn\tNNP\t*\t-\t-\t-\t-\t*\t(1)
                test_doc\t0\t1\tsaid\tVBD\t*\t-\t-\t-\t-\t*\t-
                test_doc\t0\t2\the\tPRP\t*\t-\t-\t-\t-\t*\t(1)

                #end document
                """;

        Conll2012Parser.ParseResult result = parser.parse(content, testDocumentId);

        Map<Integer, List<Conll2012Parser.MentionSpan>> chains = result.getCoreferenceChains();
        assertTrue(chains.containsKey(1));
        assertEquals(2, chains.get(1).size());

        // First mention "John"
        assertEquals(0, chains.get(1).get(0).getStartTokenIndex());
        assertEquals(0, chains.get(1).get(0).getEndTokenIndex());

        // Second mention "he"
        assertEquals(2, chains.get(1).get(1).getStartTokenIndex());
        assertEquals(2, chains.get(1).get(1).getEndTokenIndex());
    }

    @Test
    @DisplayName("Should parse multi-token coreference")
    void parseMultiTokenCoreference() throws IOException {
        String content = """
                #begin document (test_doc); part 000
                test_doc\t0\t0\tNew\tNNP\t*\t-\t-\t-\t-\t*\t(2
                test_doc\t0\t1\tYork\tNNP\t*\t-\t-\t-\t-\t*\t2)
                test_doc\t0\t2\tis\tVBZ\t*\t-\t-\t-\t-\t*\t-
                test_doc\t0\t3\tnice\tJJ\t*\t-\t-\t-\t-\t*\t-

                #end document
                """;

        Conll2012Parser.ParseResult result = parser.parse(content, testDocumentId);

        Map<Integer, List<Conll2012Parser.MentionSpan>> chains = result.getCoreferenceChains();
        assertTrue(chains.containsKey(2));
        assertEquals(1, chains.get(2).size());

        // "New York" spans tokens 0-1
        assertEquals(0, chains.get(2).get(0).getStartTokenIndex());
        assertEquals(1, chains.get(2).get(0).getEndTokenIndex());
    }

    @Test
    @DisplayName("Should set correct token indices")
    void setCorrectTokenIndices() throws IOException {
        String content = """
                #begin document (test_doc); part 000
                test_doc\t0\t0\tWord1\tNN\t*\t-\t-\t-\t-\t*\t-
                test_doc\t0\t1\tWord2\tNN\t*\t-\t-\t-\t-\t*\t-

                test_doc\t0\t0\tWord3\tNN\t*\t-\t-\t-\t-\t*\t-

                #end document
                """;

        Conll2012Parser.ParseResult result = parser.parse(content, testDocumentId);

        List<TokenEntity> tokens = result.getTokens();
        assertEquals(3, tokens.size());

        // Global indices
        assertEquals(0, tokens.get(0).getGlobalIndex());
        assertEquals(1, tokens.get(1).getGlobalIndex());
        assertEquals(2, tokens.get(2).getGlobalIndex());

        // Sentence indices
        assertEquals(0, tokens.get(0).getSentenceIndex());
        assertEquals(0, tokens.get(1).getSentenceIndex());
        assertEquals(1, tokens.get(2).getSentenceIndex());

        // Token indices within sentence
        assertEquals(0, tokens.get(0).getTokenIndex());
        assertEquals(1, tokens.get(1).getTokenIndex());
        assertEquals(0, tokens.get(2).getTokenIndex()); // Reset for new sentence
    }

    @Test
    @DisplayName("Should handle empty content")
    void handleEmptyContent() throws IOException {
        Conll2012Parser.ParseResult result = parser.parse("", testDocumentId);

        assertTrue(result.getSentences().isEmpty());
        assertTrue(result.getTokens().isEmpty());
    }

    @Test
    @DisplayName("Should skip comment lines")
    void skipCommentLines() throws IOException {
        String content = """
                #begin document (test_doc); part 000
                # This is a comment
                test_doc\t0\t0\tWord\tNN\t*\t-\t-\t-\t-\t*\t-

                #end document
                """;

        Conll2012Parser.ParseResult result = parser.parse(content, testDocumentId);

        assertEquals(1, result.getTokens().size());
    }

    @Test
    @DisplayName("Should parse Assamese Unicode tokens with codepoint-based offsets")
    void parseAssameseUnicode() throws IOException {
        // "ৰামে কৈছিল" = "Ram said" — Assamese (Bengali script).
        // Each visual character is a single codepoint in BMP, but
        // length() counts UTF-16 code units. For these particular
        // chars the code-point and char count happen to coincide,
        // but the parser must use codePointCount so supplementary-plane
        // text would also be counted correctly.
        String content = "#begin document (assam); part 000\n"
                + "assam\t0\t0\tৰামে\t-\t*\t-\t-\t-\t-\t*\t-\n"
                + "assam\t0\t1\tকৈছিল\t-\t*\t-\t-\t-\t-\t*\t-\n"
                + "\n"
                + "#end document\n";

        Conll2012Parser.ParseResult result = parser.parse(content, testDocumentId);

        assertEquals(2, result.getTokens().size());
        TokenEntity t0 = result.getTokens().get(0);
        TokenEntity t1 = result.getTokens().get(1);
        assertEquals("ৰামে", t0.getForm());
        assertEquals("কৈছিল", t1.getForm());

        // Each token's offset span equals its codepoint count, not its UTF-16
        // char count. (For Assamese chars these happen to coincide, but the
        // assertion enforces the codePointCount path.)
        int t0Cp = t0.getForm().codePointCount(0, t0.getForm().length());
        int t1Cp = t1.getForm().codePointCount(0, t1.getForm().length());
        assertEquals(t0Cp, t0.getEndOffset() - t0.getStartOffset());
        assertEquals(t1Cp, t1.getEndOffset() - t1.getStartOffset());
    }

    @Test
    @DisplayName("Should throw ValidationException for non-integer token-index column")
    void rejectMalformedIntegerColumn() {
        String content = "#begin document (bad); part 000\n"
                + "bad\t0\tABC\tWord\tNN\t*\t-\t-\t-\t-\t*\t-\n"
                + "\n"
                + "#end document\n";

        ValidationException ex = assertThrows(ValidationException.class,
                () -> parser.parse(content, testDocumentId));
        assertTrue(ex.getMessage().contains("ABC"), () -> "Expected message to mention bad token: " + ex.getMessage());
        assertTrue(ex.getMessage().toLowerCase().contains("line"), () -> "Expected line number in message: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should parse a real Train.conll fixture chunk without errors")
    void parseTrainConllFixture() throws IOException {
        String content;
        try (var in = getClass().getResourceAsStream("/conll/train-sample.conll")) {
            assertNotNull(in, "fixture missing: /conll/train-sample.conll");
            content = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        Conll2012Parser.ParseResult result = parser.parse(content, testDocumentId);

        assertFalse(result.getTokens().isEmpty(), "should produce tokens");
        assertFalse(result.getSentences().isEmpty(), "should produce sentences");
        // The sample chunk includes lines like "(625)" and "(101)" coref annotations
        assertFalse(result.getCoreferenceChains().isEmpty(),
                "should produce mention chains from the (N) annotations in the sample");
    }

    @Test
    @DisplayName("Round-trip: parse -> export -> parse preserves mention spans")
    void roundTripMentionsThroughExporter() throws IOException {
        // Two-sentence doc: two mentions, both refer to cluster 1.
        String original = "#begin document (rt); part 000\n"
                + "rt\t0\t0\tBob\tNNP\t*\t-\t-\t-\t-\t*\t(1)\n"
                + "rt\t0\t1\tlaughed\tVB\t*\t-\t-\t-\t-\t*\t-\n"
                + "\n"
                + "rt\t0\t0\tHe\tPRP\t*\t-\t-\t-\t-\t*\t(1)\n"
                + "rt\t0\t1\twon\tVB\t*\t-\t-\t-\t-\t*\t-\n"
                + "\n"
                + "#end document\n";

        Conll2012Parser.ParseResult parsed = parser.parse(original, testDocumentId);
        assertEquals(2, parsed.getCoreferenceChains().get(1).size());

        // Build the coref-annotation map the way CoreferenceService.generateCorefAnnotations does.
        Map<String, String> coref = new HashMap<>();
        parsed.getCoreferenceChains().forEach((clusterId, spans) -> {
            for (Conll2012Parser.MentionSpan s : spans) {
                if (s.getStartTokenIndex() == s.getEndTokenIndex()) {
                    coref.put(s.getSentenceIndex() + "-" + s.getStartTokenIndex(), "(" + clusterId + ")");
                } else {
                    coref.merge(s.getSentenceIndex() + "-" + s.getStartTokenIndex(),
                            "(" + clusterId, (a, b) -> a + "|" + b);
                    coref.merge(s.getSentenceIndex() + "-" + s.getEndTokenIndex(),
                            clusterId + ")", (a, b) -> a + "|" + b);
                }
            }
        });

        Map<Integer, List<TokenEntity>> bySent = new HashMap<>();
        for (TokenEntity tok : parsed.getTokens()) {
            bySent.computeIfAbsent(tok.getSentenceIndex(), k -> new ArrayList<>()).add(tok);
        }

        String exported = new Conll2012Exporter().export(
                "rt", parsed.getSentences(), bySent, coref, new ExportOptions(), 0);

        // Re-parse the exporter's output; mention chain count should be preserved
        Conll2012Parser.ParseResult reparsed = parser.parse(exported, testDocumentId);
        assertEquals(1, reparsed.getCoreferenceChains().size(), "exactly one cluster expected");
        assertEquals(2, reparsed.getCoreferenceChains().values().iterator().next().size(),
                "two mentions expected after round-trip");
    }

    @Test
    @DisplayName("Should throw ValidationException for non-integer cluster id in coref column")
    void rejectMalformedCorefAnnotation() {
        String content = "#begin document (badcoref); part 000\n"
                + "badcoref\t0\t0\tFoo\t-\t*\t-\t-\t-\t-\t*\t(abc)\n"
                + "\n"
                + "#end document\n";

        ValidationException ex = assertThrows(ValidationException.class,
                () -> parser.parse(content, testDocumentId));
        assertTrue(ex.getMessage().contains("abc"), () -> "Expected message to mention bad annotation: " + ex.getMessage());
    }
}
