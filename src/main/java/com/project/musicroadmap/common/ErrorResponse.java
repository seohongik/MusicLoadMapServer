package com.project.musicroadmap.common;

/** 기획서 9장 에러 형식: { "code": "NO_PATH", "message": "..." } */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }
}
