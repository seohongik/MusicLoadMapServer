package com.project.musicroadmap.genre.intro;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NoIntroSource implements ArtistIntroSource {

    @Override
    public IntroSourceType type() {
        return IntroSourceType.NONE;
    }

    @Override
    public Optional<ArtistIntro> fetch(String mbid) {
        return Optional.empty();
    }
}
