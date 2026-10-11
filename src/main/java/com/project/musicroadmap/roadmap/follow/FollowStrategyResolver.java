package com.project.musicroadmap.roadmap.follow;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 새 방식 = enum 1개 + @Component 1개 (기획서 5.0) */
@Component
public class FollowStrategyResolver {

    private final Map<FollowMode, FollowStrategy> strategies = new EnumMap<>(FollowMode.class);

    public FollowStrategyResolver(List<FollowStrategy> strategies) {
        strategies.forEach(s -> this.strategies.put(s.mode(), s));
    }

    public FollowStrategy get(FollowMode mode) {
        return strategies.get(mode);
    }
}
