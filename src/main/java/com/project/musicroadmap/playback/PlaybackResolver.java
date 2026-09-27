package com.project.musicroadmap.playback;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 설정된 재생 방식의 전략을 돌려준다. 새 방식 = enum 1개 + @Component 1개 */
@Component
public class PlaybackResolver {

    private final Map<PlaybackType, PlaybackStrategy> strategies = new EnumMap<>(PlaybackType.class);
    private final PlaybackType configured;

    public PlaybackResolver(List<PlaybackStrategy> strategies,
                            @Value("${app.playback.strategy:NONE}") PlaybackType configured) {
        strategies.forEach(s -> this.strategies.put(s.type(), s));
        this.configured = configured;
    }

    public PlaybackStrategy current() {
        return get(configured);
    }

    public PlaybackStrategy get(PlaybackType type) {
        PlaybackStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No PlaybackStrategy for " + type);
        }
        return strategy;
    }
}
