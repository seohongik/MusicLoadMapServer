package com.project.musicroadmap.me;

import com.project.musicroadmap.auth.LoginUser;
import com.project.musicroadmap.auth.UserService;
import com.project.musicroadmap.listening.ListeningLogRepository;
import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.PreferenceService;
import com.project.musicroadmap.preference.PreferenceService.PreferenceResponse;
import com.project.musicroadmap.preference.TargetType;
import com.project.musicroadmap.roadmap.RoadmapDtos.LogResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.RoadmapResponse;
import com.project.musicroadmap.roadmap.RoadmapService;
import com.project.musicroadmap.roadmap.selection.TrackSelectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 개인 레이어 (기획서 15, 16장) */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final RoadmapService roadmapService;
    private final PreferenceService preferenceService;
    private final ListeningLogRepository logRepository;
    private final UserService userService;

    @GetMapping("/roadmaps")
    public List<RoadmapResponse> roadmaps(@LoginUser Long userId) {
        return roadmapService.myRoadmaps(userId);
    }

    @GetMapping("/logs")
    @Transactional(readOnly = true)
    public List<LogResponse> logs(@LoginUser Long userId) {
        return logRepository.findByUserIdOrderByListenedAtDesc(userId).stream().map(LogResponse::from).toList();
    }

    @GetMapping("/preferences")
    public List<PreferenceResponse> preferences(@LoginUser Long userId) {
        return preferenceService.list(userId);
    }

    @PutMapping("/preferences")
    public PreferenceResponse savePreference(@LoginUser Long userId, @Valid @RequestBody PreferenceRequest request) {
        return preferenceService.save(userId, request.targetType(), request.targetId(), request.preference());
    }

    @DeleteMapping("/preferences/{targetType}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePreference(@LoginUser Long userId, @PathVariable TargetType targetType, @PathVariable Long targetId) {
        preferenceService.delete(userId, targetType, targetId);
    }

    @GetMapping("/settings")
    public SettingsResponse settings(@LoginUser Long userId) {
        return new SettingsResponse(userService.getTrackSelectionType(userId), userService.usesMyEdges(userId));
    }

    /** 보낸 값만 바꾼다 (null이면 그대로) */
    @PatchMapping("/settings")
    public SettingsResponse updateSettings(@LoginUser Long userId, @RequestBody SettingsRequest request) {
        if (request.trackSelectionType() != null) {
            userService.changeTrackSelectionType(userId, request.trackSelectionType());
        }
        if (request.useMyEdges() != null) {
            userService.changeUseMyEdges(userId, request.useMyEdges());
        }
        return settings(userId);
    }

    public record PreferenceRequest(@NotNull TargetType targetType, @NotNull Long targetId, @NotNull Preference preference) {
    }

    public record SettingsRequest(TrackSelectionType trackSelectionType, Boolean useMyEdges) {
    }

    /** useMyEdges: 내가 추가한 연결을 로드맵 계산에 쓸지 (기획서 15.2 규칙 5) */
    public record SettingsResponse(TrackSelectionType trackSelectionType, boolean useMyEdges) {
    }
}
