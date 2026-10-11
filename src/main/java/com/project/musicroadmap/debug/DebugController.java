package com.project.musicroadmap.debug;

import com.project.musicroadmap.common.AppClock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** local·h2 프로필 전용: 하루가 지난 것처럼 서버 시계를 옮긴다 (맛보기·하루 1곡 잠금 테스트용) */
@Profile({"local", "h2"})
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final AppClock clock;
    private final com.project.musicroadmap.relation.RelationBatch relationBatch;

    @PostMapping("/skip-day")
    public SkipDayResponse skipDay() {
        clock.skipDay();
        return new SkipDayResponse(clock.today());
    }

    public record SkipDayResponse(LocalDate today) {
    }

    /** 기획서 24장: 큐레이션 가수와 그 멤버들의 관계를 백그라운드로 가져온다 (초당 1회 제한 때문에 오래 걸림) */
    @PostMapping("/relations/sync")
    public RelationSyncResponse syncRelations() {
        return new RelationSyncResponse(relationBatch.startIfIdle());
    }

    public record RelationSyncResponse(boolean started) {
    }
}
