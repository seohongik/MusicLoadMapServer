package com.project.musicroadmap.roadmap.exploration;

import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.weight.UserWeights;
import java.util.List;

/** 기획서 5.2: 출발 장르를 포함한 장르 ID 목록을 돌려준다 */
public interface ExplorationStrategy {

    ExplorationType type();

    List<Long> explore(GenreGraph graph, UserWeights weights, ExplorationRequest request);
}
