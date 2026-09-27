package com.project.musicroadmap.preference.handler;

import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.TargetType;

/** 기획서 16.4: 대상 종류마다 검증과 후속 처리가 다르다 */
public interface PreferenceHandler {

    TargetType type();

    /** 대상이 없으면 TARGET_NOT_FOUND */
    void validate(Long targetId);

    /** 내 취향 목록에 보여줄 이름 (예: "Wonderwall · Oasis"). 대상이 없으면 null */
    String describe(Long targetId);

    /** 저장된 뒤 추가로 할 일 (기본: 없음) */
    default void afterSaved(Long userId, Long targetId, Preference preference) {
    }
}
