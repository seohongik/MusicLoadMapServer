package com.project.musicroadmap.roadmap.exploration;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 후손 찾기: 간선 정방향 (기원 → 파생).
 * 고른 장르가 막다른 곳이면 지나온 장르로 돌아가 다른 갈래로 이어 간다.
 * 예: 비밥 → 쿨 재즈(자식 없음) → 비밥으로 돌아가 → 하드 밥 → 프리 재즈 …
 * (돌아간 뒤 다음 장르는 직전 장르와 간선으로 이어져 있지 않을 수 있다 — 옆 갈래)
 */
@Component
public class DescendantsStrategy extends GreedyWalkStrategy {

    @Override
    public ExplorationType type() {
        return ExplorationType.DESCENDANTS;
    }

    @Override
    protected List<Long> candidates(GenreGraph graph, long genreId) {
        return graph.children(genreId);
    }

    @Override
    protected boolean backtrackOnDeadEnd() {
        return true;
    }
}
