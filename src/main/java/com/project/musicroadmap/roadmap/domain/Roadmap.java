package com.project.musicroadmap.roadmap.domain;

import com.project.musicroadmap.auth.User;
import com.project.musicroadmap.roadmap.exploration.ExplorationType;
import com.project.musicroadmap.roadmap.follow.FollowMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 장르 ID는 메모리 그래프의 ID를 그대로 쓴다 (계보는 참조 레이어라 바뀌지 않음) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExplorationType explorationType;

    @Column(nullable = false)
    private Long startGenreId;

    /** BRIDGE일 때만 */
    private Long targetGenreId;

    @Column(nullable = false)
    private int maxSteps;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoadmapStatus status;

    @Column(nullable = false)
    private int currentStepIndex;

    /** 재계산될 때마다 +1 */
    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    /** 공유 코드 (기획서 21장). 공유하지 않으면 null */
    @Column(unique = true, length = 12)
    private String shareCode;

    private LocalDateTime sharedAt;

    /** 다른 유저의 로드맵을 따라가는 중이면 그 로드맵과 방식 */
    private Long followedRoadmapId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FollowMode followMode;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepIndex ASC, version ASC")
    private List<RoadmapStep> steps = new ArrayList<>();

    /** path[0]이 출발 장르. 출발 장르는 듣지 않으므로 바로 DONE */
    public static Roadmap create(User user, ExplorationType explorationType, Long targetGenreId,
                                 int maxSteps, List<Long> path, LocalDateTime now) {
        Roadmap roadmap = new Roadmap();
        roadmap.user = user;
        roadmap.explorationType = explorationType;
        roadmap.startGenreId = path.get(0);
        roadmap.targetGenreId = targetGenreId;
        roadmap.maxSteps = maxSteps;
        roadmap.status = RoadmapStatus.IN_PROGRESS;
        roadmap.currentStepIndex = 0;
        roadmap.version = 1;
        roadmap.startedAt = now;
        for (int i = 0; i < path.size(); i++) {
            roadmap.steps.add(RoadmapStep.pending(roadmap, i, path.get(i), 1));
        }
        roadmap.steps.get(0).markStartGenre();
        return roadmap;
    }

    /** 공유된 로드맵을 따라가는 새 로드맵. maxSteps는 따라갈 경로 길이 */
    public static Roadmap follow(User user, Roadmap source, List<Long> path, FollowMode mode, LocalDateTime now) {
        Roadmap roadmap = create(user, source.explorationType, source.targetGenreId, path.size() - 1, path, now);
        roadmap.followedRoadmapId = source.id;
        roadmap.followMode = mode;
        return roadmap;
    }

    public void share(String code, LocalDateTime now) {
        if (shareCode == null) {
            this.shareCode = code;
            this.sharedAt = now;
        }
    }

    public void unshare() {
        this.shareCode = null;
        this.sharedAt = null;
    }

    public boolean isShared() {
        return shareCode != null;
    }

    public List<Long> activeGenreIds() {
        return activeSteps().stream().map(RoadmapStep::getGenreId).toList();
    }

    /** 재계산으로 빠진 스텝을 제외한 현재 경로 */
    public List<RoadmapStep> activeSteps() {
        return steps.stream()
                .filter(RoadmapStep::isActive)
                .sorted(Comparator.comparingInt(RoadmapStep::getStepIndex))
                .toList();
    }

    public Optional<RoadmapStep> activeStepAt(int stepIndex) {
        return activeSteps().stream().filter(s -> s.getStepIndex() == stepIndex).findFirst();
    }

    public RoadmapStep currentStep() {
        return activeStepAt(currentStepIndex).orElseThrow();
    }

    public List<Long> visitedGenreIds() {
        return activeSteps().stream()
                .filter(s -> s.getStepIndex() <= currentStepIndex)
                .map(RoadmapStep::getGenreId)
                .toList();
    }

    public List<Long> remainingGenreIds() {
        return activeSteps().stream()
                .filter(s -> s.getStepIndex() > currentStepIndex)
                .map(RoadmapStep::getGenreId)
                .toList();
    }

    public void moveTo(int stepIndex) {
        this.currentStepIndex = stepIndex;
    }

    /** 현재 스텝 뒤의 경로를 새 경로로 바꾼다 (기존 것은 REPLACED로 남긴다) */
    public void replaceRemaining(List<Long> newGenreIds) {
        version++;
        steps.stream()
                .filter(s -> s.isActive() && s.getStepIndex() > currentStepIndex)
                .forEach(s -> s.replace(version));
        for (int i = 0; i < newGenreIds.size(); i++) {
            steps.add(RoadmapStep.pending(this, currentStepIndex + 1 + i, newGenreIds.get(i), version));
        }
    }

    /** 이 날 판정을 마친 장르 수 ("오늘 N개 장르째" 안내용, 기획서 19장) */
    public int completedOn(LocalDate day) {
        return (int) activeSteps().stream()
                .filter(st -> st.getCompletedAt() != null && st.getCompletedAt().toLocalDate().equals(day))
                .count();
    }

    /**
     * 기획서 19장: stepIndex 스텝의 판정을 되돌린다. 판정 직전 버전(versionBefore)으로 경로를 복원하고
     * 그 스텝을 다시 진행 중으로 만든다. (가중치 복원은 서비스가 스냅샷으로 한다)
     */
    public void rollbackCompletion(int stepIndex, int versionBefore) {
        steps.removeIf(s -> s.getVersion() > versionBefore);
        steps.stream()
                .filter(s -> s.getStatus() == StepStatus.REPLACED
                        && s.getReplacedAtVersion() != null && s.getReplacedAtVersion() > versionBefore)
                .forEach(RoadmapStep::unreplace);
        activeSteps().stream()
                .filter(s -> s.getStepIndex() > stepIndex)
                .forEach(RoadmapStep::resetToPending);
        activeStepAt(stepIndex).orElseThrow().reopen();
        this.currentStepIndex = stepIndex;
        this.version = versionBefore;
    }

    public void complete(LocalDateTime now) {
        this.status = RoadmapStatus.COMPLETED;
        this.endedAt = now;
    }

    public void abandon(LocalDateTime now) {
        this.status = RoadmapStatus.ABANDONED;
        this.endedAt = now;
    }

    public boolean isInProgress() {
        return status == RoadmapStatus.IN_PROGRESS;
    }
}
