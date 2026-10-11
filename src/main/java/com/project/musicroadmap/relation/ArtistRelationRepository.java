package com.project.musicroadmap.relation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRelationRepository extends JpaRepository<ArtistRelation, Long> {

    boolean existsByFromNodeIdAndToNodeIdAndType(Long fromNodeId, Long toNodeId, RelationType type);

    List<ArtistRelation> findByFromNodeIdOrToNodeId(Long fromNodeId, Long toNodeId);
}
