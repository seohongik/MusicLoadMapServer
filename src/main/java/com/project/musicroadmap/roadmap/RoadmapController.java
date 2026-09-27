package com.project.musicroadmap.roadmap;

import com.project.musicroadmap.auth.LoginUser;
import com.project.musicroadmap.roadmap.RoadmapDtos.CreateRoadmapRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.FeedbackResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.ListenRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.NextRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.StepResultResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.VerdictChangeRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.PreviewRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.PreviewResult;
import com.project.musicroadmap.roadmap.RoadmapDtos.RoadmapResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.TodayResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    @PostMapping("/roadmaps/preview")
    public List<PreviewResult> preview(@LoginUser Long userId, @Valid @RequestBody PreviewRequest request) {
        return roadmapService.preview(userId, request);
    }

    @PostMapping("/roadmaps")
    @ResponseStatus(HttpStatus.CREATED)
    public RoadmapResponse create(@LoginUser Long userId, @Valid @RequestBody CreateRoadmapRequest request) {
        return roadmapService.create(userId, request);
    }

    /** 진행 중인 로드맵이 없으면 204 */
    @GetMapping("/roadmaps/current")
    public ResponseEntity<RoadmapResponse> current(@LoginUser Long userId) {
        return roadmapService.current(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** "다음 장르로" (기획서 19장). body가 없거나 verdict가 null이면 곡 반응 다수결 */
    @PostMapping("/roadmaps/current/next")
    public StepResultResponse next(@LoginUser Long userId, @RequestBody(required = false) NextRequest request) {
        return roadmapService.next(userId, request == null ? null : request.verdict());
    }

    /** 직전 판정 바꾸기 (기획서 19장) */
    @PostMapping("/roadmaps/current/verdict-change")
    public StepResultResponse changeVerdict(@LoginUser Long userId, @Valid @RequestBody VerdictChangeRequest request) {
        return roadmapService.changeLastVerdict(userId, request.verdict());
    }

    @PostMapping("/roadmaps/{roadmapId}/abandon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandon(@LoginUser Long userId, @PathVariable Long roadmapId) {
        roadmapService.abandon(userId, roadmapId);
    }

    /** 진행 중인 로드맵이 없으면 204 */
    @GetMapping("/today")
    public ResponseEntity<TodayResponse> today(@LoginUser Long userId) {
        return roadmapService.today(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/listens")
    public FeedbackResponse listen(@LoginUser Long userId, @Valid @RequestBody ListenRequest request) {
        return roadmapService.submitFeedback(userId, request);
    }
}
