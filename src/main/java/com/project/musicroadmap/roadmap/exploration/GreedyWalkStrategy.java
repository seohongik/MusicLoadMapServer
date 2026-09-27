package com.project.musicroadmap.roadmap.exploration;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.weight.UserWeights;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 매 단계 가장 싼 후보로 한 칸씩 이동한다. 동률: 비용 → 등장 시기 → ID
 * backtrackOnDeadEnd()가 true면, 막다른 장르에 닿았을 때 지나온 장르로 한 칸씩 돌아가
 * 아직 안 간 다른 갈래에서 이어 간다 (기획서 5.2 후손 찾기 개선).
 */
public abstract class GreedyWalkStrategy implements ExplorationStrategy {

    protected abstract List<Long> candidates(GenreGraph graph, long genreId);

    /** 막다른 곳에서 다른 갈래로 돌아갈지 (기본: 거기서 끝) */
    protected boolean backtrackOnDeadEnd() {
        return false;
    }

    @Override
    public List<Long> explore(GenreGraph graph, UserWeights weights, ExplorationRequest request) {
        List<Long> path = new ArrayList<>(List.of(request.startGenreId()));
        Set<Long> visited = new HashSet<>(request.excluded());
        visited.add(request.startGenreId());

        while (path.size() - 1 < request.maxSteps()) {
            // 방금 간 장르부터 거꾸로, 아직 갈 곳이 남은 장르를 찾는다 (돌아가지 않는 방식이면 방금 간 장르만 본다)
            Optional<Long> next = Optional.empty();
            int lookBack = backtrackOnDeadEnd() ? path.size() : 1;
            for (int i = path.size() - 1; i >= path.size() - lookBack && next.isEmpty(); i--) {
                next = bestCandidate(graph, weights, path.get(i), visited);
            }
            if (next.isEmpty()) {
                break;
            }
            path.add(next.get());
            visited.add(next.get());
        }

        if (path.size() == 1) {
            throw new BusinessException(ErrorCode.DEAD_END);
        }
        return path;
    }

    private Optional<Long> bestCandidate(GenreGraph graph, UserWeights weights, long from, Set<Long> visited) {
        return candidates(graph, from).stream()
                .filter(id -> !visited.contains(id) && !weights.isDisliked(id))
                .min(Comparator.<Long>comparingDouble(id -> graph.baseCost(from, id) * weights.of(id))
                        .thenComparingInt(id -> graph.node(id).originDecade())
                        .thenComparingLong(id -> id));
    }
}
