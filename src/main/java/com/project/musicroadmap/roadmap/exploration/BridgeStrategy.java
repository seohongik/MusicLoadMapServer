package com.project.musicroadmap.roadmap.exploration;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.weight.UserWeights;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 두 장르 잇기: 간선 방향을 무시한 다익스트라.
 * 싫어하는 장르는 다른 길이 없을 때만 통과한다.
 * 동률 경로는 (누적 비용, 장르 ID) 순으로 꺼내고 더 작을 때만 갱신한다 (기획서 5.2).
 */
@Component
public class BridgeStrategy implements ExplorationStrategy {

    private record Entry(double cost, long genreId) {
    }

    @Override
    public ExplorationType type() {
        return ExplorationType.BRIDGE;
    }

    @Override
    public List<Long> explore(GenreGraph graph, UserWeights weights, ExplorationRequest request) {
        Long target = request.targetGenreId();
        if (target == null) {
            throw new BusinessException(ErrorCode.TARGET_REQUIRED);
        }
        if (target == request.startGenreId()) {
            throw new BusinessException(ErrorCode.SAME_GENRE);
        }
        return shortestPath(graph, weights, request, target, true)
                .or(() -> shortestPath(graph, weights, request, target, false))
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_PATH));
    }

    private Optional<List<Long>> shortestPath(GenreGraph graph, UserWeights weights, ExplorationRequest request,
                                              long target, boolean avoidDisliked) {
        long start = request.startGenreId();
        Map<Long, Double> dist = new HashMap<>(Map.of(start, 0.0));
        Map<Long, Long> prev = new HashMap<>();
        Set<Long> done = new HashSet<>();
        PriorityQueue<Entry> queue = new PriorityQueue<>(
                Comparator.comparingDouble(Entry::cost).thenComparingLong(Entry::genreId));
        queue.add(new Entry(0.0, start));

        while (!queue.isEmpty()) {
            Entry entry = queue.poll();
            long u = entry.genreId();
            if (!done.add(u)) {
                continue;
            }
            if (u == target) {
                break;
            }
            for (long v : graph.neighbors(u)) {
                if (request.excluded().contains(v)) {
                    continue;
                }
                if (avoidDisliked && v != target && weights.isDisliked(v)) {
                    continue;
                }
                double nd = entry.cost() + graph.baseCost(u, v) * weights.of(v);
                if (nd < dist.getOrDefault(v, Double.MAX_VALUE)) {
                    dist.put(v, nd);
                    prev.put(v, u);
                    queue.add(new Entry(nd, v));
                }
            }
        }

        if (!dist.containsKey(target)) {
            return Optional.empty();
        }
        List<Long> path = new ArrayList<>(List.of(target));
        while (path.get(path.size() - 1) != start) {
            path.add(prev.get(path.get(path.size() - 1)));
        }
        Collections.reverse(path);
        return Optional.of(path);
    }
}
