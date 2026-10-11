package com.project.musicroadmap.archive.view;

/** 아카이브 보기 방식 (기획서 14.3, 23장) */
public enum ArchiveViewType {
    /** 시대순: 10년 단위로 */
    TIMELINE,
    /** 장르별: 내 분류를 우선해서 */
    BY_GENRE,
    /** 태그별: 내가 붙인 태그로 */
    BY_TAG,
}
