package com.project.musicroadmap.personal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGenreOverrideRepository extends JpaRepository<UserGenreOverride, Long> {

    Optional<UserGenreOverride> findByUserIdAndArtistId(Long userId, Long artistId);

    List<UserGenreOverride> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
