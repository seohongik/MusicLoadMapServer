package com.project.musicroadmap.roadmap.exploration;

/** 탐험 방식 (기획서 5.2) */
public enum ExplorationType {
    ROOTS,
    DESCENDANTS,
    BRIDGE,
    /** 이웃 탐색: 같은 부모를 둔 형제 장르 (기획서 5.2, Phase 2) */
    NEIGHBORS,
    /** 시대순 여행: 이어지는 후손 장르를 위상 정렬 + 등장 시기 순으로 (기획서 5.2, Phase 2) */
    CHRONOLOGICAL,
}
