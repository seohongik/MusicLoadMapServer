package com.project.musicroadmap.roadmap.completion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StepCompletionResolver {

    private final Map<StepCompletionType, StepCompletionPolicy> policies = new EnumMap<>(StepCompletionType.class);
    private final StepCompletionType configured;

    public StepCompletionResolver(List<StepCompletionPolicy> policies,
                                  @Value("${app.step-completion.strategy:MANUAL_NEXT}") StepCompletionType configured) {
        policies.forEach(p -> this.policies.put(p.type(), p));
        this.configured = configured;
    }

    public StepCompletionPolicy current() {
        return policies.get(configured);
    }
}
