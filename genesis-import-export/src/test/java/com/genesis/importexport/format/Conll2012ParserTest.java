package com.genesis.importexport.format;

import static org.junit.jupiter.api.Assertions.*;

import com.genesis.importexport.entity.SentenceEntity;
import com.genesis.importexport.entity.TokenEntity;
import java.io.IOException;
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
}
