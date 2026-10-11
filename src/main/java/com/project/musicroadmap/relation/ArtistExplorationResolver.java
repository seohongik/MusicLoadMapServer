package com.project.musicroadmap.relation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ArtistExplorationResolver {

    private final Map<ArtistExplorationType, ArtistExplorationStrategy> strategies =
            new EnumMap<>(ArtistExplorationType.class);

    public ArtistExplorationResolver(List<ArtistExplorationStrategy> strategies) {
        strategies.forEach(s -> this.strategies.put(s.type(), s));
    }

    public ArtistExplorationStrategy get(ArtistExplorationType type) {
        return strategies.get(type);
    }
}
