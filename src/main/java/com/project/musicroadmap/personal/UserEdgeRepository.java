package com.project.musicroadmap.personal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEdgeRepository extends JpaRepository<UserEdge, Long> {

    List<UserEdge> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<UserEdge> findByUserIdAndFromGenreIdAndToGenreId(Long userId, Long fromGenreId, Long toGenreId);
}
