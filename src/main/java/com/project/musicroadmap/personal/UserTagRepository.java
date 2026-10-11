package com.project.musicroadmap.personal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {

    List<UserTag> findByUserIdOrderByCreatedAtAsc(Long userId);

    List<UserTag> findByUserIdAndTargetTypeAndTargetIdOrderByCreatedAtAsc(Long userId, PersonalTargetType targetType,
                                                                           Long targetId);

    List<UserTag> findByUserIdAndNameOrderByCreatedAtDesc(Long userId, String name);

    Optional<UserTag> findByUserIdAndTargetTypeAndTargetIdAndName(Long userId, PersonalTargetType targetType,
                                                                    Long targetId, String name);
}
