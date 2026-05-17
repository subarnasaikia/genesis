package com.genesis.ner.repository;

import com.genesis.ner.entity.NerAnnotationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NerAnnotationRepository extends JpaRepository<NerAnnotationEntity, UUID> {

    List<NerAnnotationEntity> findByDocumentId(UUID documentId);

    List<NerAnnotationEntity> findByDocumentIdAndAnnotatorId(UUID documentId, String annotatorId);

    void deleteByDocumentId(UUID documentId);
}
