package com.project.musicroadmap.playback;

import com.project.musicroadmap.genre.Track;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 곡마다 유튜브 검색 결과 링크 (앱에서 재생하지 않고 유튜브 앱으로 넘긴다) */
@Component
public class SearchLinkPlayback implements PlaybackStrategy {

    private static final String YOUTUBE_SEARCH = "https://www.youtube.com/results?search_query=";

    @Override
    public PlaybackType type() {
        return PlaybackType.SEARCH_LINK;
    }

    @Override
    public Optional<String> listenUrl(Track track) {
        return Optional.of(YOUTUBE_SEARCH + URLEncoder.encode(track.searchQuery(), StandardCharsets.UTF_8));
    }
}
