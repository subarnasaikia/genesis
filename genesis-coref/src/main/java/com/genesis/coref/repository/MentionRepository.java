package com.genesis.coref.repository;

import com.genesis.coref.entity.Mention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Mention entity operations.
 */
@Repository
public interface MentionRepository extends JpaRepository<Mention, UUID> {

    /**
     * Find all mentions in a cluster.
     *
     * @param clusterId the cluster ID
     * @return list of mentions
     */
    List<Mention> findByClusterId(UUID clusterId);

    /**
     * Find all mentions in a cluster, ordered by token start index.
     *
     * @param clusterId the cluster ID
     * @return list of mentions ordered by start index
     */
    List<Mention> findByClusterIdOrderByTokenStartIndexAsc(UUID clusterId);

    /**
     * Delete all mentions in a cluster.
     *
     * @param clusterId the cluster ID
     */
    void deleteByClusterId(UUID clusterId);

    /**
     * Count mentions in a cluster.
     *
     * @param clusterId the cluster ID
     * @return mention count
     */
    long countByClusterId(UUID clusterId);
}
