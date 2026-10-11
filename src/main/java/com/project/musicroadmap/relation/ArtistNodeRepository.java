package com.project.musicroadmap.relation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistNodeRepository extends JpaRepository<ArtistNode, Long> {

    Optional<ArtistNode> findByMbid(String mbid);

    Optional<ArtistNode> findByArtistId(Long artistId);

    List<ArtistNode> findByRelationsSyncedAtIsNullAndTypeOrderByIdAsc(String type);
}
