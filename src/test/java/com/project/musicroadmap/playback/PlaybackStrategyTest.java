package com.project.musicroadmap.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.Genre;
import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.genre.TrackRole;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaybackStrategyTest {

    private final Genre britpop = Genre.curated("BRITPOP", "Britpop", "브릿팝", 1990, "", 0, 0);
    private final Track wonderwall = Track.curated(britpop, Artist.curated("Oasis", britpop), "Wonderwall", 1995, TrackRole.MAIN, 0);
    private final PlaybackResolver resolver = new PlaybackResolver(
            List.of(new NoPlayback(), new SearchLinkPlayback()), PlaybackType.NONE);

    @Test
    void 기본은_소개만_듣기_링크_없음() {
        assertThat(resolver.current().type()).isEqualTo(PlaybackType.NONE);
        assertThat(resolver.current().listenUrl(wonderwall)).isEmpty();
    }

    @Test
    void 검색_링크는_가수와_곡명을_인코딩한다() {
        assertThat(resolver.get(PlaybackType.SEARCH_LINK).listenUrl(wonderwall))
                .contains("https://www.youtube.com/results?search_query=Oasis+Wonderwall");
    }

    @Test
    void 모든_재생_방식에_구현체가_있다() {
        for (PlaybackType type : PlaybackType.values()) {
            assertThat(resolver.get(type).type()).isEqualTo(type);
        }
    }
}
