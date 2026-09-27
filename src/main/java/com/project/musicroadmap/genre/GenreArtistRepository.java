package com.project.musicroadmap.genre;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreArtistRepository extends JpaRepository<GenreArtist, Long> {

    List<GenreArtist> findAllByOrderByGenreIdAscDisplayOrderAsc();

    List<GenreArtist> findByArtistIdOrderByGenreIdAsc(Long artistId);
}
