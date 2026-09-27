package com.project.musicroadmap.playback;

import com.project.musicroadmap.genre.Track;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 소개 중심: 듣기 링크 없음 */
@Component
public class NoPlayback implements PlaybackStrategy {

    @Override
    public PlaybackType type() {
        return PlaybackType.NONE;
    }

    @Override
    public Optional<String> listenUrl(Track track) {
        return Optional.empty();
    }
}
