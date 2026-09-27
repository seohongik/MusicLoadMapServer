package com.project.musicroadmap.roadmap.exploration;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 스프링이 ExplorationStrategy 구현체를 모두 주입한다. 새 전략 = enum 1개 + @Component 1개 (기획서 5.0) */
@Component
public class ExplorationStrategyResolver {

    private final Map<ExplorationType, ExplorationStrategy> strategies = new EnumMap<>(ExplorationType.class);

    public ExplorationStrategyResolver(List<ExplorationStrategy> strategies) {
        strategies.forEach(s -> this.strategies.put(s.type(), s));
    }

    public ExplorationStrategy get(ExplorationType type) {
        ExplorationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_EXPLORATION);
        }
        return strategy;
    }
}
