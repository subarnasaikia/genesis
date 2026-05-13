package com.genesis.wsd.repository;

import com.genesis.wsd.entity.WsdAnnotationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WsdAnnotationRepository extends JpaRepository<WsdAnnotationEntity, UUID> {

    long countBySenseId(UUID senseId);

    Optional<WsdAnnotationEntity> findByTokenIdAndAnnotatorId(UUID tokenId, String annotatorId);

    List<WsdAnnotationEntity> findByTokenId(UUID tokenId);

    List<WsdAnnotationEntity> findByWorkspaceId(UUID workspaceId);

    void deleteByTokenIdAndAnnotatorId(UUID tokenId, String annotatorId);
}
