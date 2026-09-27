package com.project.musicroadmap.playback;

/** 기획서 17장: 재생 방식. 설정 app.playback.strategy로 고른다 */
public enum PlaybackType {
    /** 소개만. 듣기 링크를 주지 않는다 (기본) */
    NONE,
    /** 곡마다 유튜브 검색 결과로 연결 */
    SEARCH_LINK,
}
