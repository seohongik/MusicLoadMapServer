package com.project.musicroadmap.relation;

import java.util.Map;
import java.util.Optional;

/** 탐험에 쓰는 아티스트 관계 (기획서 24장). 트리뷰트 밴드·이름 변경·가족 관계 등은 쓰지 않는다 */
public enum RelationType {
    /** 사람 → 밴드 (from이 to의 멤버) */
    MEMBER("멤버"),
    /** 함께 작업 (방향 없음) */
    COLLABORATION("협업");

    private final String label;

    RelationType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    private static final Map<String, RelationType> FROM_MUSICBRAINZ = Map.of(
            "member of band", MEMBER,
            "subgroup", MEMBER,
            "collaboration", COLLABORATION,
            "supporting musician", COLLABORATION,
            "vocal supporting musician", COLLABORATION,
            "instrumental supporting musician", COLLABORATION);

    public static Optional<RelationType> fromMusicBrainz(String type) {
        return Optional.ofNullable(FROM_MUSICBRAINZ.get(type));
    }
}
