package com.project.musicroadmap.genre.intro;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ArtistIntroResolver {

    private final Map<IntroSourceType, ArtistIntroSource> sources = new EnumMap<>(IntroSourceType.class);
    private final IntroSourceType configured;

    public ArtistIntroResolver(List<ArtistIntroSource> sources,
                               @Value("${app.intro.source:NONE}") IntroSourceType configured) {
        sources.forEach(s -> this.sources.put(s.type(), s));
        this.configured = configured;
    }

    public ArtistIntroSource current() {
        return sources.get(configured);
    }
}
