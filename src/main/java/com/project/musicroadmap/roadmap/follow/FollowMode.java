package com.project.musicroadmap.roadmap.follow;

/** 다른 유저의 로드맵을 따라가는 방식 (기획서 21장) */
public enum FollowMode {
    /** 경로를 그대로 따라간다. 판정은 내 가중치에만 반영하고 경로는 바꾸지 않는다 */
    AS_IS,
    /** 내가 싫어하는 장르는 빼고 시작하고, 판정에 따라 원래 탐험 방식으로 남은 경로를 다시 짠다 */
    ADAPT
}
