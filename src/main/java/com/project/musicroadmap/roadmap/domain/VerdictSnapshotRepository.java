package com.project.musicroadmap.roadmap.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerdictSnapshotRepository extends JpaRepository<VerdictSnapshot, Long> {

    Optional<VerdictSnapshot> findByRoadmapId(Long roadmapId);
}
