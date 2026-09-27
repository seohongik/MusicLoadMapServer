package com.project.musicroadmap.preference.handler;

import com.project.musicroadmap.preference.TargetType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PreferenceHandlerResolver {

    private final Map<TargetType, PreferenceHandler> handlers = new EnumMap<>(TargetType.class);

    public PreferenceHandlerResolver(List<PreferenceHandler> handlers) {
        handlers.forEach(h -> this.handlers.put(h.type(), h));
    }

    public PreferenceHandler get(TargetType type) {
        PreferenceHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalStateException("No PreferenceHandler for " + type);
        }
        return handler;
    }
}
