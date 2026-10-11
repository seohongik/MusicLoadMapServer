package com.project.musicroadmap.genre.intro;

/** 가수 소개 글과 출처 (위키백과는 CC BY-SA라 출처 표기 필수, 기획서 14.1) */
public record ArtistIntro(String text, String sourceUrl, String sourceLabel) {
}
