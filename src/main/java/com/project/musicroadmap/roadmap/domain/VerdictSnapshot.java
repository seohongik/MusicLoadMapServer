package com.project.musicroadmap.roadmap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기획서 19장: 마지막 판정 "직전" 상태. 로드맵마다 하나만 두고 판정할 때마다 덮어쓴다.
 * 판정을 바꿀 때 계산으로 되돌리지 않고(상한·하한 때문에 정확하지 않음) 이 값으로 복원한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerdictSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long roadmapId;

    /** 판정한 스텝 */
    @Column(nullable = false)
    private int stepIndex;

    /** 판정 직전의 로드맵 버전 */
    @Column(nullable = false)
    private int versionBefore;

    /** 판정 직전의 장르 가중치 "장르ID:값;장르ID:값" */
    @Column(nullable = false, length = 4000)
    private String weights;

    public static VerdictSnapshot of(Long roadmapId, int stepIndex, int versionBefore, Map<Long, Double> weights) {
        VerdictSnapshot snapshot = new VerdictSnapshot();
        snapshot.roadmapId = roadmapId;
        snapshot.overwrite(stepIndex, versionBefore, weights);
        return snapshot;
    }

    public void overwrite(int stepIndex, int versionBefore, Map<Long, Double> weights) {
        this.stepIndex = stepIndex;
        this.versionBefore = versionBefore;
        this.weights = weights.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(";"));
    }

    public Map<Long, Double> weightMap() {
        Map<Long, Double> map = new HashMap<>();
        if (weights.isBlank()) {
            return map;
        }
        for (String pair : weights.split(";")) {
            String[] kv = pair.split(":");
            map.put(Long.parseLong(kv[0]), Double.parseDouble(kv[1]));
        }
        return map;
    }
}
