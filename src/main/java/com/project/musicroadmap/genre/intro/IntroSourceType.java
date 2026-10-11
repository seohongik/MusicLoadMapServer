package com.project.musicroadmap.genre.intro;

/** 가수 소개 글을 어디서 가져올지 (기획서 14.2 MusicDataSource) */
public enum IntroSourceType {
    /** 가져오지 않음 (테스트·오프라인) */
    NONE,
    /** 위키백과: 한국어 문서가 있으면 한국어, 없으면 영어 */
    WIKIPEDIA,
}
