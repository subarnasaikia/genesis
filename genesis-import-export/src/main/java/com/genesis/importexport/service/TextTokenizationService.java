package com.genesis.importexport.service;

import com.genesis.coref.entity.Token;
import com.genesis.coref.repository.TokenRepository;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for tokenizing text into individual tokens with character offsets.
 *
 * <p>
 * This service handles the core tokenization logic, breaking text into tokens
 * while preserving character offset information. Supports Unicode/UTF-8 for
 * Assamese and other Indic languages.
 */
@Service
public class TextTokenizationService {

    private static final Logger logger = LoggerFactory.getLogger(TextTokenizationService.class);

    private final TokenRepository tokenRepository;
    private final DocumentRepository documentRepository;

    // Pattern for tokenization: matches sequences of word characters (including Unicode)
    // or individual punctuation marks
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\p{L}+|\\p{N}+|\\p{Punct}",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    public TextTokenizationService(
            TokenRepository tokenRepository,
            DocumentRepository documentRepository) {
        this.tokenRepository = tokenRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Tokenize text and return a list of token objects with offsets.
     * Does not persist to database - use tokenizeAndSave for that.
     *
     * @param text the text to tokenize
     * @return list of tokens with text and offsets
     */
    public List<TokenInfo> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<TokenInfo> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text);

        while (matcher.find()) {
            String tokenText = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            tokens.add(new TokenInfo(tokenText, start, end));
        }

        logger.debug("Tokenized text into {} tokens", tokens.size());
        return tokens;
    }

    /**
     * Tokenize a document and save tokens to the database.
     * Tokens are assigned continuous indices starting from the document's
     * tokenStartIndex.
     *
     * @param documentId the document ID
     * @param text       the document text to tokenize
     * @return list of saved token entities
     */
    @Transactional
    public List<Token> tokenizeAndSave(UUID documentId, String text) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Delete existing tokens for this document
        tokenRepository.deleteByDocumentId(documentId);

        // Tokenize the text
        List<TokenInfo> tokenInfos = tokenize(text);

        // Get the starting token index for this document
        Integer startIndex = document.getTokenStartIndex();
        if (startIndex == null) {
            startIndex = 0;
        }

        // Create and save token entities
        List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < tokenInfos.size(); i++) {
            TokenInfo info = tokenInfos.get(i);

            Token token = new Token();
            token.setDocument(document);
            token.setTokenIndex(startIndex + i);
            token.setText(info.text);
            token.setStartOffset(info.startOffset);
            token.setEndOffset(info.endOffset);

            tokens.add(token);
        }

        // Batch save tokens
        List<Token> savedTokens = tokenRepository.saveAll(tokens);

        // Update document token indices
        if (!savedTokens.isEmpty()) {
            int firstIndex = savedTokens.get(0).getTokenIndex();
            int lastIndex = savedTokens.get(savedTokens.size() - 1).getTokenIndex();
            document.setTokenStartIndex(firstIndex);
            document.setTokenEndIndex(lastIndex);
            documentRepository.save(document);
        }

        logger.info("Tokenized and saved {} tokens for document {}", savedTokens.size(), documentId);
        return savedTokens;
    }

    /**
     * Tokenize all documents in a workspace with continuous token numbering.
     * Documents are processed in order (by orderIndex), and tokens are numbered
     * continuously across document boundaries.
     *
     * @param workspaceId the workspace ID
     * @return total number of tokens created
     */
    @Transactional
    public int tokenizeWorkspace(UUID workspaceId) {
        // This will be implemented after we add a method to get documents by workspace
        // For now, return a placeholder
        logger.info("Tokenizing workspace {}", workspaceId);
        return 0;
    }

    /**
     * Get all tokens for a document.
     *
     * @param documentId the document ID
     * @return list of tokens ordered by index
     */
    public List<Token> getTokensForDocument(UUID documentId) {
        return tokenRepository.findByDocumentIdOrderByTokenIndexAsc(documentId);
    }

    /**
     * Data class representing token information (text and offsets).
     */
    public static class TokenInfo {
        private final String text;
        private final int startOffset;
        private final int endOffset;

        public TokenInfo(String text, int startOffset, int endOffset) {
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
            return String.format("Token{text='%s', start=%d, end=%d}", text, startOffset, endOffset);
        }
    }
}
