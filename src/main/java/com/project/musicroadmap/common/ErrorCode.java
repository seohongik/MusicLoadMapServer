package com.project.musicroadmap.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 요청
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않아요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."),

    // 탐험 (기획서 9장)
    NO_PATH(HttpStatus.BAD_REQUEST, "선택한 두 장르를 잇는 경로가 없어요."),
    SAME_GENRE(HttpStatus.BAD_REQUEST, "출발 장르와 목표 장르가 같아요."),
    DEAD_END(HttpStatus.BAD_REQUEST, "이 방향으로는 더 갈 장르가 없어요. 다른 탐험 방식을 골라 보세요."),
    TARGET_REQUIRED(HttpStatus.BAD_REQUEST, "목표 장르를 골라 주세요."),
    UNSUPPORTED_EXPLORATION(HttpStatus.BAD_REQUEST, "지원하지 않는 방식이에요."),
    GENRE_NOT_FOUND(HttpStatus.NOT_FOUND, "장르를 찾을 수 없어요."),

    // 로드맵 진행
    ROADMAP_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "이미 진행 중인 로드맵이 있어요."),
    NO_ACTIVE_ROADMAP(HttpStatus.NOT_FOUND, "진행 중인 로드맵이 없어요."),
    ROADMAP_NOT_FOUND(HttpStatus.NOT_FOUND, "로드맵을 찾을 수 없어요."),
    TRACK_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "지금은 이 곡에 반응을 남길 수 없어요."),
    ALREADY_RATED(HttpStatus.CONFLICT, "이미 반응을 남긴 곡이에요."),
    STEP_COMPLETION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "지금 설정에서는 배정된 곡에 모두 반응해야 다음 장르로 넘어갈 수 있어요."),
    VERDICT_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "바꿀 수 있는 판정이 없어요. 직전 장르의 판정만, 다음 장르에서 반응하기 전까지 바꿀 수 있어요."),

    // 선호 (기획서 16장)
    TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "선호를 저장할 대상을 찾을 수 없어요."),

    // 외부 데이터 (기획서 14장)
    EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "음악 정보 서비스(MusicBrainz)에 연결할 수 없어요. 잠시 뒤 다시 시도해 주세요."),
    EXTERNAL_ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "MusicBrainz에서 해당 아티스트를 찾을 수 없어요.");

    private final HttpStatus status;
    private final String message;
}
