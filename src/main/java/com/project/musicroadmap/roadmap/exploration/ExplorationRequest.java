package com.project.musicroadmap.roadmap.exploration;

import java.util.Set;

/**
 * @param excluded 방문하면 안 되는 장르 (이미 지나온 장르). 출발 장르는 넣지 않는다.
 */
public record ExplorationRequest(long startGenreId, Long targetGenreId, int maxSteps, Set<Long> excluded) {

    public ExplorationRequest(long startGenreId, Long targetGenreId, int maxSteps) {
        this(startGenreId, targetGenreId, maxSteps, Set.of());
    }
}
