package com.project.musicroadmap.archive;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findByArtistIdOrderByReleaseYearAscIdAsc(Long artistId);

    Optional<Album> findByMbid(String mbid);

    @EntityGraph(attributePaths = "artist")
    Optional<Album> findWithArtistById(Long id);
}
