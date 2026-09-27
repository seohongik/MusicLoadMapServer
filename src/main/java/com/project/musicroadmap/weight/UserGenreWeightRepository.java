package com.project.musicroadmap.weight;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGenreWeightRepository extends JpaRepository<UserGenreWeight, Long> {

    List<UserGenreWeight> findByUserId(Long userId);
}
