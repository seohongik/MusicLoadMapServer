package com.project.musicroadmap.roadmap.selection;

/** 곡 고르기 방식 (기획서 16.4). 유저 설정으로 고른다 */
public enum TrackSelectionType {
    EXCLUDE_DISLIKED,
    FAVORITE_ARTIST_FIRST,
    /** 장르의 가수 중 무작위. 좋아하는 가수는 먼저, 싫어하는 가수는 제외 */
    RANDOM,
}
