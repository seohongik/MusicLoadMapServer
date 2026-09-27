package com.project.musicroadmap.listening;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListeningLogRepository extends JpaRepository<ListeningLog, Long> {

    List<ListeningLog> findByRoadmapIdOrderByListenedAtAsc(Long roadmapId);

    List<ListeningLog> findByUserIdOrderByListenedAtDesc(Long userId);
}
