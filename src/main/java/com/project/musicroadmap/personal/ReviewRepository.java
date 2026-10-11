package com.project.musicroadmap.personal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndTargetTypeAndTargetId(Long userId, PersonalTargetType targetType, Long targetId);

    List<Review> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
