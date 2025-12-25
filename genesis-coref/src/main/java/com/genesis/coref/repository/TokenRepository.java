package com.genesis.coref.repository;

import com.genesis.coref.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Token entity operations.
 */
@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    /**
     * Find all tokens for a specific document, ordered by token index.
     *
     * @param documentId the document ID
     * @return list of tokens ordered by index
     */
    List<Token> findByDocumentIdOrderByTokenIndexAsc(UUID documentId);

    /**
     * Find a token by document and token index.
     *
     * @param documentId the document ID
     * @param tokenIndex the token index
     * @return the token, or null if not found
     */
    Token findByDocumentIdAndTokenIndex(UUID documentId, Integer tokenIndex);

    /**
     * Delete all tokens for a document.
     *
     * @param documentId the document ID
     */
    void deleteByDocumentId(UUID documentId);

    /**
     * Count tokens for a document.
     *
     * @param documentId the document ID
     * @return token count
     */
    long countByDocumentId(UUID documentId);
}
