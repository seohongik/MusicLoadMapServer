package com.project.musicroadmap.genre;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByCode(String code);

    List<Genre> findAllByOrderByIdAsc();
}
