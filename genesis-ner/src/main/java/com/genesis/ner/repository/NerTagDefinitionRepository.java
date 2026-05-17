package com.genesis.ner.repository;

import com.genesis.ner.entity.NerTagDefinitionEntity;
import com.genesis.ner.entity.NerTagScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NerTagDefinitionRepository extends JpaRepository<NerTagDefinitionEntity, UUID> {

    List<NerTagDefinitionEntity> findByScope(NerTagScope scope);

    List<NerTagDefinitionEntity> findByWorkspaceId(UUID workspaceId);

    Optional<NerTagDefinitionEntity> findByTagAndScopeAndWorkspaceId(
            String tag, NerTagScope scope, UUID workspaceId);

    Optional<NerTagDefinitionEntity> findByTagAndScopeAndWorkspaceIdIsNull(
            String tag, NerTagScope scope);
}
