package com.genesis.pos.repository;

import com.genesis.pos.entity.PosTagDefinitionEntity;
import com.genesis.pos.entity.PosTagScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosTagDefinitionRepository extends JpaRepository<PosTagDefinitionEntity, UUID> {

    List<PosTagDefinitionEntity> findByScope(PosTagScope scope);

    List<PosTagDefinitionEntity> findByWorkspaceId(UUID workspaceId);

    Optional<PosTagDefinitionEntity> findByTagAndScopeAndWorkspaceId(
            String tag, PosTagScope scope, UUID workspaceId);

    Optional<PosTagDefinitionEntity> findByTagAndScopeAndWorkspaceIdIsNull(
            String tag, PosTagScope scope);
}
