package com.project.musicroadmap.relation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 주변 가수: 관계를 따라 가까운 순(개인 비용 합)으로. 밴드 → 멤버 → 다른 밴드처럼 사람을 거쳐 이어진다.
 * 결과에는 밴드와 우리 DB에 있는 가수만 남긴다 (멤버 개인은 경유지). 싫어하는 가수는 지나가지도, 보여주지도 않는다.
 */
@Component
public class NearbyArtists implements ArtistExplorationStrategy {

    /** 밴드 → 멤버 → 다른 밴드 (비용 2) 정도까지 */
    static final double MAX_DISTANCE = 3.0;

    private record Entry(long id, double distance) {
    }

    @Override
    public ArtistExplorationType type() {
        return ArtistExplorationType.NEARBY;
    }

    @Override
    public List<Long> explore(ArtistGraph graph, ArtistWeights weights, long start, Long target, int limit) {
        Map<Long, Double> dist = new HashMap<>(Map.of(start, 0.0));
        PriorityQueue<Entry> queue = new PriorityQueue<>(
                Comparator.comparingDouble(Entry::distance).thenComparingLong(Entry::id));
        queue.add(new Entry(start, 0.0));
        Set<Long> done = new HashSet<>();
        List<Long> result = new ArrayList<>();
        while (!queue.isEmpty() && result.size() < limit) {
            long u = queue.poll().id();
            if (!done.add(u)) {
                continue;
            }
            ArtistNode node = graph.node(u);
            if (u != start && node != null && (node.isGroup() || node.getArtistId() != null)) {
                result.add(u);
            }
            for (ArtistGraph.Link link : graph.links(u)) {
                ArtistNode next = graph.node(link.to());
                if (weights.isDisliked(next)) {
                    continue;
                }
                double nd = dist.get(u) + weights.cost(next);
                if (nd <= MAX_DISTANCE && nd < dist.getOrDefault(link.to(), Double.MAX_VALUE)) {
                    dist.put(link.to(), nd);
                    queue.add(new Entry(link.to(), nd));
                }
            }
        }
        return result;
    }
}
