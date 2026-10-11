package com.project.musicroadmap.archive.view;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ArchiveViewResolver {

    private final Map<ArchiveViewType, ArchiveViewStrategy> strategies = new EnumMap<>(ArchiveViewType.class);

    public ArchiveViewResolver(List<ArchiveViewStrategy> strategies) {
        strategies.forEach(s -> this.strategies.put(s.type(), s));
    }

    public ArchiveViewStrategy get(ArchiveViewType type) {
        return strategies.get(type);
    }

    public List<ArchiveViewType> available() {
        return List.copyOf(strategies.keySet());
    }
}
