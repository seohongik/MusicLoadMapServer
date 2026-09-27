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

    @PostMapping("/skip-day")
    public SkipDayResponse skipDay() {
        clock.skipDay();
        return new SkipDayResponse(clock.today());
    }

    public record SkipDayResponse(LocalDate today) {
    }
}
