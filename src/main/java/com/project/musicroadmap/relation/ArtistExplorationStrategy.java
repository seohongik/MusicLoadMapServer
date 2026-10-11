package com.project.musicroadmap.relation;

import java.util.List;

/** 5.2의 탐험 전략을 가수 관계에도 적용한다 (기획서 14.3, 24장). 새 방식 = enum 1개 + @Component 1개 */
public interface ArtistExplorationStrategy {

    ArtistExplorationType type();

    /** 노드 ID 목록. NEARBY는 출발 노드 제외 가까운 순, BRIDGE는 출발~목표 경로. 결과가 없으면 빈 목록 */
    List<Long> explore(ArtistGraph graph, ArtistWeights weights, long start, Long target, int limit);
}
