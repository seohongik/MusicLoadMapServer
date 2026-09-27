package com.project.musicroadmap.roadmap.selection;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TrackSelectionResolver {

    private final Map<TrackSelectionType, TrackSelectionStrategy> strategies = new EnumMap<>(TrackSelectionType.class);

    public TrackSelectionResolver(List<TrackSelectionStrategy> strategies) {
        strategies.forEach(s -> this.strategies.put(s.type(), s));
    }

    public TrackSelectionStrategy get(TrackSelectionType type) {
        TrackSelectionStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No TrackSelectionStrategy for " + type);
        }
        return strategy;
    }
}
