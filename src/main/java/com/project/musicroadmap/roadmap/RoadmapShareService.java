package com.project.musicroadmap.roadmap;

import com.project.musicroadmap.common.AppClock;
import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.roadmap.RoadmapDtos.ShareResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.SharedRoadmapResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.SharedStep;
import com.project.musicroadmap.roadmap.domain.Roadmap;
import com.project.musicroadmap.roadmap.domain.RoadmapRepository;
import com.project.musicroadmap.roadmap.domain.StepStatus;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기획서 21장: 로드맵 공유. 개인 레이어는 기본 비공개이고(15.1), 주인이 공유를 켠 로드맵만
 * 경로와 장르별 판정이 공개된다. 곡 반응·감상은 공개하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RoadmapShareService {

    /** 헷갈리는 글자(0, O, 1, I)는 뺀다 */
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final RoadmapRepository roadmapRepository;
    private final AppClock clock;
    private final SecureRandom random = new SecureRandom();

    /** 이미 공유 중이면 같은 코드를 돌려준다 */
    public ShareResponse share(Long userId, Long roadmapId) {
        Roadmap roadmap = mine(userId, roadmapId);
        if (!roadmap.isShared()) {
            roadmap.share(newCode(), clock.now());
        }
        return new ShareResponse(roadmap.getShareCode());
    }

    public void unshare(Long userId, Long roadmapId) {
        mine(userId, roadmapId).unshare();
    }

    @Transactional(readOnly = true)
    public List<SharedRoadmapResponse> recent(Long userId) {
        return roadmapRepository.findTop30ByShareCodeIsNotNullOrderBySharedAtDesc().stream()
                .map(r -> toResponse(r, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public SharedRoadmapResponse get(Long userId, String shareCode) {
        return roadmapRepository.findByShareCode(normalize(shareCode))
                .map(r -> toResponse(r, userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARED_ROADMAP_NOT_FOUND));
    }

    static String normalize(String shareCode) {
        return shareCode == null ? "" : shareCode.trim().toUpperCase();
    }

    private Roadmap mine(Long userId, Long roadmapId) {
        return roadmapRepository.findByIdAndUserId(roadmapId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROADMAP_NOT_FOUND));
    }

    private String newCode() {
        while (true) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            if (!roadmapRepository.existsByShareCode(code.toString())) {
                return code.toString();
            }
        }
    }

    private SharedRoadmapResponse toResponse(Roadmap r, Long userId) {
        List<SharedStep> steps = r.activeSteps().stream()
                .map(s -> new SharedStep(s.getGenreId(), s.getStatus() == StepStatus.DONE ? s.getVerdict() : null))
                .toList();
        return new SharedRoadmapResponse(r.getShareCode(), r.getUser().getNickname(), r.getUser().getId().equals(userId),
                r.getExplorationType(), r.getTargetGenreId(), r.getStatus(), steps, r.getSharedAt(),
                roadmapRepository.countByFollowedRoadmapId(r.getId()));
    }
}
