package com.genesis.importexport.service;

import com.genesis.importexport.entity.SentenceEntity;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.format.Conll2012Parser;
import com.genesis.importexport.repository.SentenceRepository;
import com.genesis.importexport.repository.TokenRepository;
import com.genesis.importexport.tokenizer.SentenceSegmenter;
import com.genesis.importexport.tokenizer.SentenceSegmenter.SentenceResult;
import com.genesis.importexport.tokenizer.Tokenizer;
import com.genesis.importexport.tokenizer.Tokenizer.TokenResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for importing documents and tokenizing their content.
 */
@Service
public class ImportService {

    private final TokenRepository tokenRepository;
    private final SentenceRepository sentenceRepository;
    private final Tokenizer tokenizer;
    private final SentenceSegmenter sentenceSegmenter;
    private final Conll2012Parser conll2012Parser;

    public ImportService(TokenRepository tokenRepository,
            SentenceRepository sentenceRepository,
            Tokenizer tokenizer,
            SentenceSegmenter sentenceSegmenter) {
        this.tokenRepository = tokenRepository;
        this.sentenceRepository = sentenceRepository;
        this.tokenizer = tokenizer;
        this.sentenceSegmenter = sentenceSegmenter;
        this.conll2012Parser = new Conll2012Parser();
    }

    /**
     * Result of importing a document.
     */
    public static class ImportResult {
        private final int sentenceCount;
        private final int tokenCount;

        public ImportResult(int sentenceCount, int tokenCount) {
            this.sentenceCount = sentenceCount;
            this.tokenCount = tokenCount;
        }

        public int getSentenceCount() {
            return sentenceCount;
        }

        public int getTokenCount() {
            return tokenCount;
        }
    }

    /**
     * Import plain text content and tokenize it.
     *
     * @param documentId the document UUID
     * @param content    the text content to tokenize
     * @return import result with counts
     */
    @Transactional
    public ImportResult importPlainText(UUID documentId, String content) {
        // Clear any existing tokens/sentences for this document
        tokenRepository.deleteByDocumentId(documentId);
        sentenceRepository.deleteByDocumentId(documentId);

        if (content == null || content.trim().isEmpty()) {
            return new ImportResult(0, 0);
        }

        // Segment into sentences
        List<SentenceResult> sentenceResults = sentenceSegmenter.segment(content);

        List<SentenceEntity> sentences = new ArrayList<>();
        List<TokenEntity> tokens = new ArrayList<>();
        int globalIndex = 0;

        for (int sentenceIndex = 0; sentenceIndex < sentenceResults.size(); sentenceIndex++) {
            SentenceResult sentenceResult = sentenceResults.get(sentenceIndex);

            // Tokenize this sentence
            List<TokenResult> tokenResults = tokenizer.tokenize(
                    sentenceResult.getText(),
                    sentenceResult.getStartOffset());

            // Create sentence entity
            SentenceEntity sentence = new SentenceEntity();
            sentence.setDocumentId(documentId);
            sentence.setSentenceIndex(sentenceIndex);
            sentence.setText(sentenceResult.getText());
            sentence.setStartOffset(sentenceResult.getStartOffset());
            sentence.setEndOffset(sentenceResult.getEndOffset());
            sentence.setTokenCount(tokenResults.size());
            sentences.add(sentence);

            // Create token entities
            for (int tokenIndex = 0; tokenIndex < tokenResults.size(); tokenIndex++) {
                TokenResult tokenResult = tokenResults.get(tokenIndex);

                TokenEntity token = new TokenEntity();
                token.setDocumentId(documentId);
                token.setSentenceIndex(sentenceIndex);
                token.setTokenIndex(tokenIndex);
                token.setGlobalIndex(globalIndex);
                token.setForm(tokenResult.getText());
                token.setStartOffset(tokenResult.getStartOffset());
                token.setEndOffset(tokenResult.getEndOffset());
                tokens.add(token);

                globalIndex++;
            }
        }

        // Save all
        sentenceRepository.saveAll(sentences);
        tokenRepository.saveAll(tokens);

        return new ImportResult(sentences.size(), tokens.size());
    }

    /**
     * Import a CoNLL-2012 formatted file.
     *
     * @param documentId the document UUID
     * @param content    the CoNLL-2012 content
     * @return import result with counts
     */
    @Transactional
    public ImportResult importConll2012(UUID documentId, String content) throws IOException {
        // Clear any existing tokens/sentences for this document
        tokenRepository.deleteByDocumentId(documentId);
        sentenceRepository.deleteByDocumentId(documentId);

        if (content == null || content.trim().isEmpty()) {
            return new ImportResult(0, 0);
        }

        Conll2012Parser.ParseResult parseResult = conll2012Parser.parse(content, documentId);

        // Save all
        sentenceRepository.saveAll(parseResult.getSentences());
        tokenRepository.saveAll(parseResult.getTokens());

        return new ImportResult(
                parseResult.getSentences().size(),
                parseResult.getTokens().size());
    }

    /**
     * Get all tokens for a document.
     */
    public List<TokenEntity> getTokens(UUID documentId) {
        return tokenRepository.findByDocumentIdOrderByGlobalIndexAsc(documentId);
    }

    /**
     * Get all sentences for a document.
     */
    public List<SentenceEntity> getSentences(UUID documentId) {
        return sentenceRepository.findByDocumentIdOrderBySentenceIndexAsc(documentId);
    }

    /**
     * Get tokens for a specific sentence.
     */
    public List<TokenEntity> getTokensForSentence(UUID documentId, int sentenceIndex) {
        return tokenRepository.findByDocumentIdAndSentenceIndexOrderByTokenIndexAsc(
                documentId, sentenceIndex);
    }

    /**
     * Check if a document has been tokenized.
     */
    public boolean isTokenized(UUID documentId) {
        return tokenRepository.existsByDocumentId(documentId);
    }

    /**
     * Get token count for a document.
     */
    public long getTokenCount(UUID documentId) {
        return tokenRepository.countByDocumentId(documentId);
    }

    /**
     * Get sentence count for a document.
     */
    public long getSentenceCount(UUID documentId) {
        return sentenceRepository.countByDocumentId(documentId);
    }
}
