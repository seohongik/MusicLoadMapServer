package com.project.musicroadmap.genre;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {

    @EntityGraph(attributePaths = "artist")
    List<Track> findByGenreIdOrderByDisplayOrderAsc(Long genreId);

    @EntityGraph(attributePaths = "artist")
    List<Track> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "artist")
    Optional<Track> findWithArtistById(Long id);

    Optional<Track> findByTitle(String title);
}
