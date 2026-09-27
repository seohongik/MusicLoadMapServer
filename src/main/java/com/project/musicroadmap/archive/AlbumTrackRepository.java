package com.project.musicroadmap.archive;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumTrackRepository extends JpaRepository<AlbumTrack, Long> {

    List<AlbumTrack> findByAlbumIdOrderByDiscNumberAscTrackNumberAsc(Long albumId);

    @EntityGraph(attributePaths = "album")
    List<AlbumTrack> findByIdIn(List<Long> ids);
}
