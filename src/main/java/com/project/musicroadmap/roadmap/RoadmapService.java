package com.project.musicroadmap.roadmap;

import com.project.musicroadmap.auth.User;
import com.project.musicroadmap.auth.UserRepository;
import com.project.musicroadmap.common.AppClock;
import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.genre.GenreGraphProvider;
import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.genre.TrackRepository;
import com.project.musicroadmap.listening.DislikeReason;
import com.project.musicroadmap.listening.Feedback;
import com.project.musicroadmap.listening.GenreVerdictPolicy;
import com.project.musicroadmap.listening.ListeningLog;
import com.project.musicroadmap.listening.ListeningLogRepository;
import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.PreferenceService;
import com.project.musicroadmap.preference.TargetType;
import com.project.musicroadmap.preference.UserPreferences;
import com.project.musicroadmap.roadmap.RoadmapDtos.CreateRoadmapRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.FeedbackResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.ListenRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.PreviewRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.PreviewResult;
import com.project.musicroadmap.roadmap.RoadmapDtos.RoadmapResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.StepResultResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.TodayResponse;
import com.project.musicroadmap.roadmap.completion.StepCompletionPolicy;
import com.project.musicroadmap.roadmap.completion.StepCompletionResolver;
import com.project.musicroadmap.roadmap.domain.Roadmap;
import com.project.musicroadmap.roadmap.domain.RoadmapRepository;
import com.project.musicroadmap.roadmap.domain.RoadmapStatus;
import com.project.musicroadmap.roadmap.domain.RoadmapStep;
import com.project.musicroadmap.roadmap.domain.StepStatus;
import com.project.musicroadmap.roadmap.domain.Verdict;
import com.project.musicroadmap.roadmap.domain.VerdictSnapshot;
import com.project.musicroadmap.roadmap.domain.VerdictSnapshotRepository;
import com.project.musicroadmap.roadmap.exploration.ExplorationRequest;
import com.project.musicroadmap.roadmap.exploration.ExplorationStrategyResolver;
import com.project.musicroadmap.roadmap.exploration.ExplorationType;
import com.project.musicroadmap.roadmap.selection.TrackSelection;
import com.project.musicroadmap.roadmap.selection.TrackSelectionResolver;
import com.project.musicroadmap.weight.UserWeights;
import com.project.musicroadmap.weight.WeightService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기획서 5장·19장의 규칙. (듣기 방식·하루 제한은 소개 중심 전환 후 제거) */
@Service
@RequiredArgsConstructor
@Transactional
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final ListeningLogRepository logRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final VerdictSnapshotRepository snapshotRepository;
    private final GenreGraphProvider graphProvider;
    private final WeightService weightService;
    private final PreferenceService preferenceService;
    private final ExplorationStrategyResolver explorationResolver;
    private final TrackSelectionResolver selectionResolver;
    private final StepCompletionResolver completionResolver;
    private final GenreVerdictPolicy verdictPolicy;
    private final AppClock clock;

    @Transactional(readOnly = true)
    public List<PreviewResult> preview(Long userId, PreviewRequest request) {
        GenreGraph graph = graphProvider.get();
        graph.node(request.startGenreId());
        UserWeights weights = weightService.load(userId);

        List<PreviewResult> results = new ArrayList<>();
        for (ExplorationType type : ExplorationType.values()) {
            if (type == ExplorationType.BRIDGE && request.targetGenreId() == null) {
                continue;
            }
            try {
                List<Long> path = explorationResolver.get(type).explore(graph, weights,
                        new ExplorationRequest(request.startGenreId(), request.targetGenreId(), request.maxSteps()));
                results.add(new PreviewResult(type, path, null, null));
            } catch (BusinessException e) {
                results.add(new PreviewResult(type, List.of(), e.getErrorCode().name(), e.getMessage()));
            }
        }
        return results;
    }

    public RoadmapResponse create(Long userId, CreateRoadmapRequest request) {
        if (roadmapRepository.existsByUserIdAndStatus(userId, RoadmapStatus.IN_PROGRESS)) {
            throw new BusinessException(ErrorCode.ROADMAP_ALREADY_IN_PROGRESS);
        }
        GenreGraph graph = graphProvider.get();
        graph.node(request.startGenreId());
        Long target = request.explorationType() == ExplorationType.BRIDGE ? request.targetGenreId() : null;
        if (target != null) {
            graph.node(target);
        }

        List<Long> path = explorationResolver.get(request.explorationType()).explore(
                graph, weightService.load(userId),
                new ExplorationRequest(request.startGenreId(), target, request.maxSteps()));

        User user = userRepository.getReferenceById(userId);
        Roadmap roadmap = Roadmap.create(user, request.explorationType(), target,
                request.maxSteps(), path, clock.now());
        activateNext(roadmap, userId);
        return RoadmapResponse.from(roadmapRepository.save(roadmap));
    }

    @Transactional(readOnly = true)
    public Optional<RoadmapResponse> current(Long userId) {
        return findInProgress(userId).map(RoadmapResponse::from);
    }

    @Transactional(readOnly = true)
    public List<RoadmapResponse> myRoadmaps(Long userId) {
        return roadmapRepository.findByUserIdOrderByStartedAtAsc(userId).stream()
                .map(RoadmapResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TodayResponse> today(Long userId) {
        return findInProgress(userId).map(roadmap -> {
            RoadmapStep step = roadmap.currentStep();
            List<ListeningLog> logs = roadmapLogs(roadmap);
            Optional<VerdictSnapshot> snapshot = snapshotRepository.findByRoadmapId(roadmap.getId());
            return new TodayResponse(roadmap.getId(), step.getStepIndex(), step.getGenreId(),
                    List.copyOf(step.getTrackIds()), suggestedVerdict(step, logs),
                    snapshot.map(s -> canChangeVerdict(roadmap, s, logs)).orElse(false),
                    snapshot.map(VerdictSnapshot::getStepIndex).orElse(null),
                    roadmap.completedOn(clock.today()));
        });
    }

    /**
     * 곡 반응 저장 (기획서 5.4, 19장).
     * - 오늘 들을 수 있는 현재 장르의 곡, 또는 이미 지나온 장르의 곡에 남길 수 있다 (되돌아보기)
     * - 같은 곡에 다시 보내면 반응을 고친다
     * - 판정은 스텝 완료 방식에 따라: ALL_RATED면 모두 반응했을 때 자동, MANUAL_NEXT면 "다음 장르로"에서
     */
    public FeedbackResponse submitFeedback(Long userId, ListenRequest request) {
        Roadmap roadmap = findInProgress(userId).orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_ROADMAP));
        RoadmapStep current = roadmap.currentStep();
        List<ListeningLog> logs = roadmapLogs(roadmap);
        Optional<ListeningLog> existing = logs.stream().filter(l -> l.getTrackId().equals(request.trackId())).findFirst();
        RoadmapStep target = resolveTargetStep(roadmap, current, request.trackId());
        boolean reviewed = target != current;

        DislikeReason reason = request.feedback() == Feedback.DISLIKE
                ? (request.dislikeReason() == null ? DislikeReason.TRACK : request.dislikeReason())
                : null;
        String memo = request.memo() == null ? "" : request.memo().trim();
        if (existing.isPresent()) {
            existing.get().update(request.feedback(), reason, memo);
        } else {
            logRepository.save(ListeningLog.of(userRepository.getReferenceById(userId), roadmap.getId(),
                    request.trackId(), target.getGenreId(), request.feedback(), reason, memo, clock.now()));
        }

        // "이 가수가 별로예요": 가수 싫어함 선호를 저장한다 (기획서 16.3)
        Long dislikedArtistId = null;
        if (reason == DislikeReason.ARTIST) {
            Track track = trackRepository.findWithArtistById(request.trackId()).orElseThrow();
            dislikedArtistId = track.getArtist().getId();
            preferenceService.save(userId, TargetType.ARTIST, dislikedArtistId, Preference.DISLIKE);
        }

        boolean spareAdded = !reviewed && existing.isEmpty() && request.feedback() == Feedback.MEH && current.useSpare();

        List<ListeningLog> currentLogs = logsOf(roadmapLogs(roadmap), current);
        if (!reviewed) {
            Optional<Verdict> auto = completionResolver.current().autoVerdict(current, currentLogs);
            if (auto.isPresent()) {
                StepResultResponse result = completeCurrentStep(roadmap, userId, auto.get());
                return new FeedbackResponse(current.getGenreId(), result.verdict(), result.roadmapChanged(), spareAdded,
                        result.roadmapCompleted(), dislikedArtistId, result.roadmapVersion(), null, false);
            }
        }
        return new FeedbackResponse(target.getGenreId(), null, false, spareAdded, false, dislikedArtistId,
                roadmap.getVersion(), suggestedVerdict(current, currentLogs), reviewed);
    }

    /** "다음 장르로" (기획서 19장). verdict가 없으면 곡 반응 다수결, 반응도 없으면 중립 */
    public StepResultResponse next(Long userId, Verdict verdict) {
        Roadmap roadmap = findInProgress(userId).orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_ROADMAP));
        StepCompletionPolicy policy = completionResolver.current();
        if (!policy.allowsManualNext()) {
            throw new BusinessException(ErrorCode.STEP_COMPLETION_NOT_ALLOWED);
        }
        List<ListeningLog> logs = roadmapLogs(roadmap);
        Verdict decided = verdict != null ? verdict
                : Optional.ofNullable(suggestedVerdict(roadmap.currentStep(), logs)).orElse(Verdict.NEUTRAL);
        return completeCurrentStep(roadmap, userId, decided);
    }

    /**
     * 직전 판정 바꾸기 (기획서 19장, 좁은 범위).
     * 마지막으로 판정한 장르만, 그 뒤 장르에서 아직 반응하지 않았을 때만 가능하다.
     * 판정 직전 스냅샷(가중치, 로드맵 버전)으로 복원한 뒤 새 판정을 다시 적용한다.
     */
    public StepResultResponse changeLastVerdict(Long userId, Verdict verdict) {
        Roadmap roadmap = findInProgress(userId).orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_ROADMAP));
        VerdictSnapshot snapshot = snapshotRepository.findByRoadmapId(roadmap.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VERDICT_CHANGE_NOT_ALLOWED));
        if (!canChangeVerdict(roadmap, snapshot, roadmapLogs(roadmap))) {
            throw new BusinessException(ErrorCode.VERDICT_CHANGE_NOT_ALLOWED);
        }
        roadmap.rollbackCompletion(snapshot.getStepIndex(), snapshot.getVersionBefore());
        weightService.restore(userId, snapshot.weightMap());
        roadmapRepository.flush();
        return completeCurrentStep(roadmap, userId, verdict);
    }

    public void abandon(Long userId, Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findByIdAndUserId(roadmapId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROADMAP_NOT_FOUND));
        if (roadmap.isInProgress()) {
            roadmap.abandon(clock.now());
        }
    }

    // ---------------------------------------------------------------------------------------------

    private Optional<Roadmap> findInProgress(Long userId) {
        return roadmapRepository.findFirstByUserIdAndStatus(userId, RoadmapStatus.IN_PROGRESS);
    }

    private List<ListeningLog> roadmapLogs(Roadmap roadmap) {
        return logRepository.findByRoadmapIdOrderByListenedAtAsc(roadmap.getId());
    }

    private static List<ListeningLog> logsOf(List<ListeningLog> logs, RoadmapStep step) {
        return logs.stream().filter(l -> l.getGenreId().equals(step.getGenreId())).toList();
    }

    /** 반응이 없으면 null. 가수 이유 별로는 중립으로 센다 (5.4) */
    private Verdict suggestedVerdict(RoadmapStep step, List<ListeningLog> logs) {
        List<ListeningLog> stepLogs = logsOf(logs, step);
        return stepLogs.isEmpty() ? null : verdictPolicy.judge(stepLogs);
    }

    /** 반응을 남길 수 있는 스텝: 현재 스텝의 곡, 또는 지나온 스텝의 곡(되돌아보기) */
    private RoadmapStep resolveTargetStep(Roadmap roadmap, RoadmapStep current, Long trackId) {
        if (current.getTrackIds().contains(trackId)) {
            return current;
        }
        return roadmap.activeSteps().stream()
                .filter(s -> s.getStepIndex() >= 1 && s.getStepIndex() < roadmap.getCurrentStepIndex())
                .filter(s -> s.getStatus() == StepStatus.DONE && s.getTrackIds().contains(trackId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_AVAILABLE));
    }

    /** 스냅샷의 스텝이 여전히 판정 상태이고, 그 뒤 장르들에서 아직 반응이 없을 때만 */
    private boolean canChangeVerdict(Roadmap roadmap, VerdictSnapshot snapshot, List<ListeningLog> logs) {
        Optional<RoadmapStep> judged = roadmap.activeStepAt(snapshot.getStepIndex());
        if (judged.isEmpty() || judged.get().getStatus() != StepStatus.DONE
                || snapshot.getStepIndex() >= roadmap.getCurrentStepIndex()) {
            return false;
        }
        Set<Long> laterGenres = new HashSet<>();
        roadmap.activeSteps().stream()
                .filter(s -> s.getStepIndex() > snapshot.getStepIndex())
                .forEach(s -> laterGenres.add(s.getGenreId()));
        return logs.stream().noneMatch(l -> laterGenres.contains(l.getGenreId()));
    }

    /** 현재 스텝 판정: 스냅샷 저장 → 가중치 → 재계산 → 다음 스텝 */
    private StepResultResponse completeCurrentStep(Roadmap roadmap, Long userId, Verdict verdict) {
        RoadmapStep step = roadmap.currentStep();
        GenreGraph graph = graphProvider.get();
        UserWeights weights = weightService.load(userId);

        // 판정을 바꿀 수 있도록 "판정 직전" 상태를 저장한다
        snapshotRepository.findByRoadmapId(roadmap.getId()).ifPresentOrElse(
                s -> s.overwrite(step.getStepIndex(), roadmap.getVersion(), weights.snapshot()),
                () -> snapshotRepository.save(VerdictSnapshot.of(roadmap.getId(), step.getStepIndex(),
                        roadmap.getVersion(), weights.snapshot())));

        if (verdict == Verdict.LIKED) {
            weights.applyLike(step.getGenreId(), graph);
        } else if (verdict == Verdict.DISLIKED) {
            weights.applyDislike(step.getGenreId(), graph);
        }
        weightService.save(userId, weights);
        step.finish(verdict, clock.now());

        boolean changed = false;
        if (verdict != Verdict.NEUTRAL) {
            int before = roadmap.getVersion();
            recalculate(roadmap, graph, weights, verdict == Verdict.DISLIKED);
            changed = roadmap.getVersion() != before;
        }

        activateNext(roadmap, userId);
        return new StepResultResponse(step.getGenreId(), verdict, changed,
                roadmap.getStatus() == RoadmapStatus.COMPLETED, roadmap.getVersion());
    }

    /**
     * 다음 스텝을 시작한다. 이때 곡 고르기 전략으로 곡을 고른다 (기획서 8장 RoadmapStep).
     * 고를 곡이 없으면(모두 싫어하는 가수) 그 스텝은 건너뛴다. 다음 스텝이 없으면 완주.
     */
    private void activateNext(Roadmap roadmap, Long userId) {
        UserPreferences preferences = preferenceService.load(userId);
        User user = userRepository.findById(userId).orElseThrow();
        while (true) {
            Optional<RoadmapStep> next = roadmap.activeStepAt(roadmap.getCurrentStepIndex() + 1);
            if (next.isEmpty()) {
                roadmap.complete(clock.now());
                return;
            }
            RoadmapStep step = next.get();
            roadmap.moveTo(step.getStepIndex());
            TrackSelection selection = selectionResolver.get(user.getTrackSelectionType())
                    .select(trackRepository.findByGenreIdOrderByDisplayOrderAsc(step.getGenreId()), preferences);
            if (selection.mainTrackIds().isEmpty()) {
                step.skip();
                continue;
            }
            step.start(selection.mainTrackIds(), selection.spareTrackId());
            return;
        }
    }

    /**
     * 기획서 5.5: 남은 경로를 같은 전략으로 다시 뽑는다.
     * - 좋아함: 방금 끝난 장르에서 이어서 계산
     * - 싫어함(가지 바꾸기): 직전 장르로 돌아가 다른 가지를 먼저 시도하고, 길이 없으면 방금 끝난 장르에서 이어서 계산
     */
    private void recalculate(Roadmap roadmap, GenreGraph graph, UserWeights weights, boolean switchBranch) {
        Long current = roadmap.currentStep().getGenreId();
        Optional<RoadmapStep> previous = roadmap.activeStepAt(roadmap.getCurrentStepIndex() - 1);
        Set<Long> visited = new HashSet<>(roadmap.visitedGenreIds());
        List<Long> oldRemaining = roadmap.remainingGenreIds();

        List<Long> origins = new ArrayList<>();
        if (switchBranch) {
            previous.ifPresent(p -> origins.add(p.getGenreId()));
        }
        origins.add(current);

        List<Long> newRemaining = null;
        for (Long from : origins) {
            newRemaining = remainingFrom(roadmap, graph, weights, from, visited);
            if (newRemaining != null) {
                break;
            }
        }
        if (newRemaining == null) {
            // 두 장르 잇기는 새 길을 못 찾으면 기존 경로를 유지하고, 나머지는 여기서 끝낸다
            newRemaining = roadmap.getExplorationType() == ExplorationType.BRIDGE ? oldRemaining : List.of();
        }
        if (!newRemaining.equals(oldRemaining)) {
            roadmap.replaceRemaining(newRemaining);
        }
    }

    /** from에서 다시 탐색한 남은 경로(from 제외). 더 갈 곳이 없으면 null */
    private List<Long> remainingFrom(Roadmap roadmap, GenreGraph graph, UserWeights weights, Long from, Set<Long> visited) {
        Set<Long> excluded = new HashSet<>(visited);
        excluded.remove(from);
        try {
            List<Long> path;
            if (roadmap.getExplorationType() == ExplorationType.BRIDGE) {
                if (from.equals(roadmap.getTargetGenreId())) {
                    return List.of();
                }
                path = explorationResolver.get(ExplorationType.BRIDGE).explore(graph, weights,
                        new ExplorationRequest(from, roadmap.getTargetGenreId(), roadmap.getMaxSteps(), excluded));
            } else {
                int remainingSteps = roadmap.getMaxSteps() - roadmap.getCurrentStepIndex();
                if (remainingSteps <= 0) {
                    return List.of();
                }
                path = explorationResolver.get(roadmap.getExplorationType()).explore(graph, weights,
                        new ExplorationRequest(from, null, remainingSteps, excluded));
            }
            return path.subList(1, path.size());
        } catch (BusinessException e) {
            return null;
        }
    }
}
