package com.project.musicroadmap.roadmap;

import com.project.musicroadmap.listening.DislikeReason;
import com.project.musicroadmap.listening.Feedback;
import com.project.musicroadmap.listening.ListeningLog;
import com.project.musicroadmap.roadmap.domain.Roadmap;
import com.project.musicroadmap.roadmap.domain.RoadmapStatus;
import com.project.musicroadmap.roadmap.domain.RoadmapStep;
import com.project.musicroadmap.roadmap.domain.StepStatus;
import com.project.musicroadmap.roadmap.domain.Verdict;
import com.project.musicroadmap.roadmap.exploration.ExplorationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class RoadmapDtos {

    private RoadmapDtos() {
    }

    public record PreviewRequest(
            @NotNull Long startGenreId,
            Long targetGenreId,
            @Min(3) @Max(8) int maxSteps) {
    }

    /** 전략별 미리보기. 성공이면 path, 실패면 errorCode·message */
    public record PreviewResult(ExplorationType explorationType, List<Long> path, String errorCode, String message) {
    }

    public record CreateRoadmapRequest(
            @NotNull ExplorationType explorationType,
            @NotNull Long startGenreId,
            Long targetGenreId,
            @Min(3) @Max(8) int maxSteps) {
    }

    public record ListenRequest(
            @NotNull Long trackId,
            @NotNull Feedback feedback,
            DislikeReason dislikeReason,
            @Size(max = 100) String memo) {
    }

    public record StepResponse(int stepIndex, Long genreId, StepStatus status, Verdict verdict,
                               List<Long> trackIds, Long spareTrackId, boolean spareUsed, int version) {
        static StepResponse from(RoadmapStep s) {
            return new StepResponse(s.getStepIndex(), s.getGenreId(), s.getStatus(), s.getVerdict(),
                    List.copyOf(s.getTrackIds()), s.getSpareTrackId(), s.isSpareUsed(), s.getVersion());
        }
    }

    public record RoadmapResponse(Long id, ExplorationType explorationType, Long startGenreId,
                                  Long targetGenreId, int maxSteps, RoadmapStatus status, int currentStepIndex,
                                  int version, List<StepResponse> steps) {
        public static RoadmapResponse from(Roadmap r) {
            return new RoadmapResponse(r.getId(), r.getExplorationType(), r.getStartGenreId(),
                    r.getTargetGenreId(), r.getMaxSteps(), r.getStatus(), r.getCurrentStepIndex(), r.getVersion(),
                    r.getSteps().stream().map(StepResponse::from).toList());
        }
    }

    /**
     * @param suggestedVerdict      이 장르에 남긴 곡 반응의 다수결 ("다음 장르로" 질문의 기본 선택값). 반응이 없으면 null
     * @param canChangeLastVerdict  직전 판정을 바꿀 수 있는지 (기획서 19장)
     * @param lastJudgedStepIndex   마지막으로 판정한 스텝 (없으면 null)
     * @param genresCompletedToday  오늘 이 로드맵에서 넘긴 장르 수. 막지 않고 "쉬어 가도 좋아요" 안내에만 쓴다
     */
    public record TodayResponse(Long roadmapId, int stepIndex, Long genreId, List<Long> trackIds,
                                Verdict suggestedVerdict, boolean canChangeLastVerdict, Integer lastJudgedStepIndex,
                                int genresCompletedToday) {
    }

    /**
     * @param genreVerdict  ALL_RATED 방식에서 자동 판정됐을 때만 값이 있다
     * @param reviewed      지나온 장르의 곡에 남긴(또는 고친) 반응인지
     */
    public record FeedbackResponse(Long genreId, Verdict genreVerdict, boolean roadmapChanged, boolean spareAdded,
                                   boolean roadmapCompleted, Long dislikedArtistId, int roadmapVersion,
                                   Verdict suggestedVerdict, boolean reviewed) {
    }

    /** "다음 장르로". verdict가 없으면 곡 반응 다수결(없으면 중립)로 판정한다 */
    public record NextRequest(Verdict verdict) {
    }

    public record VerdictChangeRequest(@NotNull Verdict verdict) {
    }

    /** 스텝 판정 결과 */
    public record StepResultResponse(Long genreId, Verdict verdict, boolean roadmapChanged, boolean roadmapCompleted,
                                     int roadmapVersion) {
    }

    public record LogResponse(Long id, Long roadmapId, Long trackId, Long genreId, Feedback feedback,
                              DislikeReason dislikeReason, String memo, LocalDateTime listenedAt) {
        public static LogResponse from(ListeningLog l) {
            return new LogResponse(l.getId(), l.getRoadmapId(), l.getTrackId(), l.getGenreId(), l.getFeedback(),
                    l.getDislikeReason(), l.getMemo(), l.getListenedAt());
        }
    }
}
