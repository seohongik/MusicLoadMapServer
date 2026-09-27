package com.project.musicroadmap.roadmap.exploration;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.weight.UserWeights;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 시대순 여행: 출발 장르에서 이어지는 후손 장르를 연대순으로 만난다.
 * 1. 출발 장르에서 정방향으로 닿는 장르를 모은다 (싫어하는 장르·이미 지나온 장르는 거치지 않음)
 * 2. 그 안에서 위상 정렬: 모은 장르 중의 부모를 모두 거친 뒤에만 자식이 나온다
 * 3. 동시에 나올 수 있는 장르가 여럿이면 등장 시기 → 비용 → ID 순
 */
@Component
public class ChronologicalStrategy implements ExplorationStrategy {

    @Override
    public ExplorationType type() {
        return ExplorationType.CHRONOLOGICAL;
    }

    @Override
    public List<Long> explore(GenreGraph graph, UserWeights weights, ExplorationRequest request) {
        long start = request.startGenreId();
        Set<Long> reachable = reachableFrom(graph, weights, request);

        // 모은 장르 안에서만 센 "아직 안 거친 부모 수"
        Map<Long, Integer> pendingParents = new HashMap<>();
        for (long genre : reachable) {
            if (genre != start) {
                pendingParents.put(genre, (int) graph.parents(genre).stream().filter(reachable::contains).count());
            }
        }

        PriorityQueue<Long> ready = new PriorityQueue<>(Comparator.<Long>comparingInt(id -> graph.node(id).originDecade())
                .thenComparingDouble(weights::of)
                .thenComparingLong(id -> id));
        List<Long> path = new ArrayList<>(List.of(start));
        release(graph, start, reachable, pendingParents, ready);

        while (!ready.isEmpty() && path.size() - 1 < request.maxSteps()) {
            long next = ready.poll();
            path.add(next);
            release(graph, next, reachable, pendingParents, ready);
        }

        if (path.size() == 1) {
            throw new BusinessException(ErrorCode.DEAD_END);
        }
        return path;
    }

    /** genre를 거쳤으니 그 자식들의 대기 부모 수를 줄이고, 0이 되면 나올 수 있다 */
    private void release(GenreGraph graph, long genre, Set<Long> reachable, Map<Long, Integer> pending, PriorityQueue<Long> ready) {
        for (long child : graph.children(genre)) {
            if (reachable.contains(child) && pending.merge(child, -1, Integer::sum) == 0) {
                ready.add(child);
            }
        }
    }

    private Set<Long> reachableFrom(GenreGraph graph, UserWeights weights, ExplorationRequest request) {
        Set<Long> reachable = new HashSet<>(List.of(request.startGenreId()));
        Deque<Long> queue = new ArrayDeque<>(List.of(request.startGenreId()));
        while (!queue.isEmpty()) {
            for (long child : graph.children(queue.poll())) {
                if (!request.excluded().contains(child) && !weights.isDisliked(child) && reachable.add(child)) {
                    queue.add(child);
                }
            }
        }
        return reachable;
    }
}
