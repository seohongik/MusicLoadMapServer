package com.project.musicroadmap.roadmap.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadmapStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_id")
    private Roadmap roadmap;

    /** 0 = 출발 장르 */
    @Column(nullable = false)
    private int stepIndex;

    @Column(nullable = false)
    private Long genreId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Verdict verdict;

    /** 스텝이 시작될 때 TrackSelectionStrategy가 고른 곡 (기획서 8장) */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "roadmap_step_track", joinColumns = @JoinColumn(name = "roadmap_step_id"))
    @OrderColumn(name = "track_order")
    @Column(name = "track_id", nullable = false)
    private List<Long> trackIds = new ArrayList<>();

    /** 애매함일 때 추가로 줄 예비곡 */
    private Long spareTrackId;

    @Column(nullable = false)
    private boolean spareUsed;

    /** 이 스텝이 만들어진 로드맵 버전 */
    @Column(nullable = false)
    private int version;

    /** 판정을 마친 시각. 맛보기(하루 1장르) 잠금에 쓴다. 건너뛴 스텝은 null */
    private LocalDateTime completedAt;

    /** 어느 버전에서 재계산으로 빠졌는지. 판정 되돌리기 때 복원 대상을 찾는 데 쓴다 */
    private Integer replacedAtVersion;

    static RoadmapStep pending(Roadmap roadmap, int stepIndex, Long genreId, int version) {
        RoadmapStep step = new RoadmapStep();
        step.roadmap = roadmap;
        step.stepIndex = stepIndex;
        step.genreId = genreId;
        step.status = StepStatus.PENDING;
        step.version = version;
        return step;
    }

    void markStartGenre() {
        this.status = StepStatus.DONE;
    }

    public void start(List<Long> mainTrackIds, Long spareTrackId) {
        this.status = StepStatus.IN_PROGRESS;
        this.trackIds.clear();
        this.trackIds.addAll(mainTrackIds);
        this.spareTrackId = spareTrackId;
    }

    /** 고를 곡이 없어서(모두 싫어하는 가수) 건너뛴다 */
    public void skip() {
        this.status = StepStatus.DONE;
        this.verdict = Verdict.NEUTRAL;
    }

    /** 애매함: 예비곡을 한 번 더 준다. 줄 수 있으면 true */
    public boolean useSpare() {
        if (spareUsed || spareTrackId == null) {
            return false;
        }
        trackIds.add(spareTrackId);
        spareUsed = true;
        return true;
    }

    public void finish(Verdict verdict, LocalDateTime now) {
        this.status = StepStatus.DONE;
        this.verdict = verdict;
        this.completedAt = now;
    }

    void replace(int atVersion) {
        this.status = StepStatus.REPLACED;
        this.replacedAtVersion = atVersion;
    }

    // --- 판정 되돌리기 (기획서 19장) ---

    /** 재계산으로 빠졌던 스텝을 다시 예정 상태로 */
    void unreplace() {
        this.status = StepStatus.PENDING;
        this.replacedAtVersion = null;
    }

    /** 판정을 취소하고 다시 진행 중으로 */
    void reopen() {
        this.status = StepStatus.IN_PROGRESS;
        this.verdict = null;
        this.completedAt = null;
    }

    /** 시작 전 상태로 (곡은 다시 시작될 때 고른다) */
    void resetToPending() {
        this.status = StepStatus.PENDING;
        this.verdict = null;
        this.completedAt = null;
        this.trackIds.clear();
        this.spareTrackId = null;
        this.spareUsed = false;
    }

    public boolean isActive() {
        return status != StepStatus.REPLACED;
    }
}
