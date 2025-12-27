package com.genesis.importexport.format;

import static org.junit.jupiter.api.Assertions.*;

import com.genesis.importexport.dto.ExportOptions;
import com.genesis.importexport.entity.SentenceEntity;
import com.genesis.importexport.entity.TokenEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Conll2012Exporter.
 */
class Conll2012ExporterTest {

    private Conll2012Exporter exporter;
    private UUID testDocumentId;

    @BeforeEach
    void setUp() {
        exporter = new Conll2012Exporter();
        testDocumentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should export basic tokens to CoNLL format")
    void exportBasicTokens() {
        List<SentenceEntity> sentences = createTestSentences();
        Map<Integer, List<TokenEntity>> tokensBySentence = createTokensBySentence();
        Map<String, String> coref = new HashMap<>();
        ExportOptions options = new ExportOptions();

        String result = exporter.export("test_doc", sentences, tokensBySentence, coref, options, 0);

        assertTrue(result.contains("#begin document"));
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("world"));
        assertTrue(result.contains("#end document"));
    }

    @Test
    @DisplayName("Should use part number mode by default")
    void usePartNumberByDefault() {
        List<SentenceEntity> sentences = createTestSentences();
        Map<Integer, List<TokenEntity>> tokensBySentence = createTokensBySentence();
        Map<String, String> coref = new HashMap<>();
        ExportOptions options = new ExportOptions();

        String result = exporter.export("test_doc", sentences, tokensBySentence, coref, options, 0);

        // Column 2 should be 0 (part number)
        String[] lines = result.split("\n");
        for (String line : lines) {
            if (!line.startsWith("#") && !line.isEmpty()) {
                String[] cols = line.split("\t");
                assertEquals("0", cols[1]); // Part number should be 0
                break;
            }
        }
    }

    @Test
    @DisplayName("Should use sentence number when configured")
    void useSentenceNumberMode() {
        List<SentenceEntity> sentences = createTestSentences();
        Map<Integer, List<TokenEntity>> tokensBySentence = createTokensBySentence();
        Map<String, String> coref = new HashMap<>();
        ExportOptions options = new ExportOptions();
        options.setColumn2Mode(ExportOptions.Column2Mode.SENTENCE_NUMBER);

        String result = exporter.export("test_doc", sentences, tokensBySentence, coref, options, 0);

        String[] lines = result.split("\n");
        for (String line : lines) {
            if (!line.startsWith("#") && !line.isEmpty()) {
                String[] cols = line.split("\t");
                // Should be sentence index (0 for first sentence)
                assertEquals("0", cols[1]);
                break;
            }
        }
    }

    @Test
    @DisplayName("Should include coreference annotations")
    void includeCorefAnnotations() {
        List<SentenceEntity> sentences = createTestSentences();
        Map<Integer, List<TokenEntity>> tokensBySentence = createTokensBySentence();
        ExportOptions options = new ExportOptions();

        Map<String, String> corefAnnotations = new HashMap<>();
        corefAnnotations.put("0-0", "(1)"); // Token 0 in sentence 0

        String result = exporter.export("test_doc", sentences, tokensBySentence, corefAnnotations, options, 0);

        assertTrue(result.contains("(1)"));
    }

    @Test
    @DisplayName("Should use dash for tokens without coref")
    void useDashForNoCoref() {
        List<SentenceEntity> sentences = createTestSentences();
        Map<Integer, List<TokenEntity>> tokensBySentence = createTokensBySentence();
        Map<String, String> coref = new HashMap<>();
        ExportOptions options = new ExportOptions();

        String result = exporter.export("test_doc", sentences, tokensBySentence, coref, options, 0);

        // Last column should be "-" when no coref
        String[] lines = result.split("\n");
        for (String line : lines) {
            if (!line.startsWith("#") && !line.isEmpty()) {
                String[] cols = line.split("\t");
                assertEquals("-", cols[cols.length - 1]);
                break;
            }
        }
    }

    @Test
    @DisplayName("Should separate sentences with blank lines")
    void separateSentencesWithBlankLines() {
        List<SentenceEntity> sentences = new ArrayList<>();
        sentences.add(createSentence(0, "First."));
        sentences.add(createSentence(1, "Second."));

        Map<Integer, List<TokenEntity>> tokensBySentence = new HashMap<>();
        tokensBySentence.put(0, createTokensForSentence(0, new String[] { "First", "." }));
        tokensBySentence.put(1, createTokensForSentence(1, new String[] { "Second", "." }));

        Map<String, String> coref = new HashMap<>();
        ExportOptions options = new ExportOptions();

        String result = exporter.export("test_doc", sentences, tokensBySentence, coref, options, 0);

        // Should have blank line between sentences
        assertTrue(result.contains("\n\n"));
    }

    @Test
    @DisplayName("Should export merged documents")
    void exportMergedDocuments() {
        List<Conll2012Exporter.DocumentExportData> documents = new ArrayList<>();

        documents.add(new Conll2012Exporter.DocumentExportData(
                "doc1",
                createTestSentences(),
                createTokensBySentence(),
                new HashMap<>()));
        documents.add(new Conll2012Exporter.DocumentExportData(
                "doc2",
                createTestSentences(),
                createTokensBySentence(),
                new HashMap<>()));

        ExportOptions options = new ExportOptions();
        String result = exporter.exportMerged(documents, options);

        // Should contain both document headers
        assertTrue(result.contains("doc1"));
        assertTrue(result.contains("doc2"));
        // Should have two end markers
        assertEquals(2, result.split("#end document").length - 1);
    }

    private Map<Integer, List<TokenEntity>> createTokensBySentence() {
        Map<Integer, List<TokenEntity>> map = new HashMap<>();
        map.put(0, createTokensForSentence(0, new String[] { "Hello", "world", "." }));
        return map;
    }

    private List<SentenceEntity> createTestSentences() {
        List<SentenceEntity> sentences = new ArrayList<>();
        sentences.add(createSentence(0, "Hello world."));
        return sentences;
    }

    private List<TokenEntity> createTokensForSentence(int sentIdx, String[] words) {
        List<TokenEntity> tokens = new ArrayList<>();
        int offset = 0;
        for (int i = 0; i < words.length; i++) {
            TokenEntity token = new TokenEntity();
            token.setDocumentId(testDocumentId);
            token.setSentenceIndex(sentIdx);
            token.setTokenIndex(i);
            token.setGlobalIndex(sentIdx * 100 + i);
            token.setForm(words[i]);
            token.setStartOffset(offset);
            token.setEndOffset(offset + words[i].length());
            tokens.add(token);
            offset += words[i].length() + 1;
        }
        return tokens;
    }

    private SentenceEntity createSentence(int sentIdx, String text) {
        SentenceEntity sentence = new SentenceEntity();
        sentence.setDocumentId(testDocumentId);
        sentence.setSentenceIndex(sentIdx);
        sentence.setText(text);
        sentence.setTokenCount(text.split("\\s+").length);
        return sentence;
    }
}
