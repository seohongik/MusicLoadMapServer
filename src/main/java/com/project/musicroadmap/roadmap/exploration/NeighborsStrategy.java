package com.project.musicroadmap.roadmap.exploration;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.weight.UserWeights;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 이웃 탐색: 같은 부모를 둔 형제 장르를 만난다.
 * 1차로 출발 장르의 형제, 자리가 남으면 그 형제들의 형제(2차)로 넓혀 간다.
 * 한 차수 안에서의 순서: 비용 → 겹치는 부모 수(많을수록 가까움) → 등장 시기 → ID
 * 형제끼리는 간선으로 직접 이어져 있지 않다 (부모를 사이에 둔 옆 가지).
 */
@Component
public class NeighborsStrategy implements ExplorationStrategy {

    @Override
    public ExplorationType type() {
        return ExplorationType.NEIGHBORS;
    }

    @Override
    public List<Long> explore(GenreGraph graph, UserWeights weights, ExplorationRequest request) {
        List<Long> path = new ArrayList<>(List.of(request.startGenreId()));
        Set<Long> visited = new HashSet<>(request.excluded());
        visited.add(request.startGenreId());

        List<Long> ring = List.of(request.startGenreId());
        while (path.size() - 1 < request.maxSteps() && !ring.isEmpty()) {
            Map<Long, Integer> sharedParents = new HashMap<>();
            for (long genre : ring) {
                for (long parent : graph.parents(genre)) {
                    for (long sibling : graph.children(parent)) {
                        if (!visited.contains(sibling) && !weights.isDisliked(sibling)) {
                            sharedParents.merge(sibling, 1, Integer::sum);
                        }
                    }
                }
            }
            List<Long> next = sharedParents.keySet().stream()
                    .sorted(Comparator.<Long>comparingDouble(weights::of)
                            .thenComparing(id -> -sharedParents.get(id))
                            .thenComparingInt(id -> graph.node(id).originDecade())
                            .thenComparingLong(id -> id))
                    .limit(request.maxSteps() - (path.size() - 1))
                    .toList();
            path.addAll(next);
            visited.addAll(next);
            ring = next;
        }

        if (path.size() == 1) {
            throw new BusinessException(ErrorCode.DEAD_END);
        }
        return path;
    }
}
