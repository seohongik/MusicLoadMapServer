package com.project.musicroadmap.genre.mapping;

/** 외부 장르 투표를 우리 장르로 모은 후보. votes = 그 장르로 대응된 외부 장르들의 투표 합 */
public record GenreCandidate(long genreId, int votes) {
}
