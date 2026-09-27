package com.project.musicroadmap.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 서버의 "지금". 모든 날짜 판정은 Asia/Seoul 기준 (기획서 1장).
 * 테스트와 local 프로필에서는 skipDay()로 하루씩 앞으로 옮길 수 있다.
 */
@Component
public class AppClock {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final AtomicLong offsetDays = new AtomicLong();

    public LocalDateTime now() {
        return LocalDateTime.now(ZONE).plusDays(offsetDays.get());
    }

    public LocalDate today() {
        return now().toLocalDate();
    }

    public void skipDay() {
        offsetDays.incrementAndGet();
    }
}
