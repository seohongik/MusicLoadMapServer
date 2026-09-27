package com.project.musicroadmap.roadmap.completion;

/** 기획서 19장: 스텝을 언제 끝난 것으로 볼지. 설정 app.step-completion.strategy로 고른다 */
public enum StepCompletionType {
    /** "다음 장르로" 버튼 + 장르 한 줄 판정. 곡 반응은 선택 (기본) */
    MANUAL_NEXT,
    /** 배정된 곡에 모두 반응하면 다수결로 자동 판정 (초기 방식) */
    ALL_RATED,
}
