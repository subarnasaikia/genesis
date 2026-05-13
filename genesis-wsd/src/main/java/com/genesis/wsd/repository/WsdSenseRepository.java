package com.genesis.wsd.repository;

import com.genesis.wsd.entity.WsdSenseEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WsdSenseRepository extends JpaRepository<WsdSenseEntity, UUID> {

    List<WsdSenseEntity> findByWorkspaceIdOrderByWordAscSenseLabelAsc(UUID workspaceId);

    List<WsdSenseEntity> findByWorkspaceIdAndWordOrderBySenseLabelAsc(UUID workspaceId, String word);
}
