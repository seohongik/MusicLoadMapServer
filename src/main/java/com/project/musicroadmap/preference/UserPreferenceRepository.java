package com.project.musicroadmap.preference;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<UserPreference> findByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, Long targetId);
}
