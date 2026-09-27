package com.project.musicroadmap.roadmap.exploration;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.List;
import org.springframework.stereotype.Component;

/** 뿌리 찾기: 간선 역방향 (파생 → 기원) */
@Component
public class RootsStrategy extends GreedyWalkStrategy {

    @Override
    public ExplorationType type() {
        return ExplorationType.ROOTS;
    }

    @Override
    protected List<Long> candidates(GenreGraph graph, long genreId) {
        return graph.parents(genreId);
    }
}
