package com.project.musicroadmap.relation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 두 가수 잇기: 다익스트라. 싫어하는 가수는 다른 길이 없을 때만 지나간다 (장르 BRIDGE와 같은 규칙) */
@Component
public class ArtistBridge implements ArtistExplorationStrategy {

    @Override
    public ArtistExplorationType type() {
        return ArtistExplorationType.BRIDGE;
    }

    @Override
    public List<Long> explore(ArtistGraph graph, ArtistWeights weights, long start, Long target, int limit) {
        if (target == null || target == start) {
            return List.of();
        }
        List<Long> avoiding = shortest(graph, weights, start, target, true);
        return avoiding.isEmpty() ? shortest(graph, weights, start, target, false) : avoiding;
    }

    private static List<Long> shortest(ArtistGraph graph, ArtistWeights weights, long start, long target,
                                       boolean avoidDisliked) {
        Map<Long, Double> dist = new HashMap<>(Map.of(start, 0.0));
        Map<Long, Long> prev = new HashMap<>();
        Set<Long> done = new HashSet<>();
        PriorityQueue<Map.Entry<Long, Double>> queue = new PriorityQueue<>(
                Comparator.comparingDouble((Map.Entry<Long, Double> e) -> e.getValue()).thenComparingLong(Map.Entry::getKey));
        queue.add(Map.entry(start, 0.0));
        while (!queue.isEmpty()) {
            long u = queue.poll().getKey();
            if (!done.add(u)) {
                continue;
            }
            if (u == target) {
                break;
            }
            for (ArtistGraph.Link link : graph.links(u)) {
                ArtistNode next = graph.node(link.to());
                if (avoidDisliked && link.to() != target && weights.isDisliked(next)) {
                    continue;
                }
                double nd = dist.get(u) + weights.cost(next);
                if (nd < dist.getOrDefault(link.to(), Double.MAX_VALUE)) {
                    dist.put(link.to(), nd);
                    prev.put(link.to(), u);
                    queue.add(Map.entry(link.to(), nd));
                }
            }
        }
        if (!dist.containsKey(target)) {
            return List.of();
        }
        List<Long> path = new ArrayList<>(List.of(target));
        while (path.get(path.size() - 1) != start) {
            path.add(prev.get(path.get(path.size() - 1)));
        }
        Collections.reverse(path);
        return path;
    }
}
