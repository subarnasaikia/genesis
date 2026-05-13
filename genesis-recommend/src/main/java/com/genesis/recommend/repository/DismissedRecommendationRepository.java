package com.genesis.recommend.repository;

import com.genesis.recommend.entity.DismissedRecommendationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DismissedRecommendationRepository
        extends JpaRepository<DismissedRecommendationEntity, UUID> {

    Optional<DismissedRecommendationEntity> findByUserIdAndRecommendationHash(UUID userId, String recommendationHash);

    List<DismissedRecommendationEntity> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);
}
