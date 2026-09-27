package com.project.musicroadmap.roadmap.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {

    Optional<Roadmap> findFirstByUserIdAndStatus(Long userId, RoadmapStatus status);

    boolean existsByUserIdAndStatus(Long userId, RoadmapStatus status);

    List<Roadmap> findByUserIdOrderByStartedAtAsc(Long userId);

    Optional<Roadmap> findByIdAndUserId(Long id, Long userId);
}
