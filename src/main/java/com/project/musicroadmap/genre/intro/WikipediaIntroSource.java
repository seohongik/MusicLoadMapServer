package com.project.musicroadmap.genre.intro;

import com.project.musicroadmap.external.musicbrainz.MusicBrainzClient;
import com.project.musicroadmap.external.wikipedia.WikipediaClient;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** MusicBrainz 링크 → 위키데이터 → 위키백과 요약 (한국어 우선, 없으면 영어) */
@Component
@RequiredArgsConstructor
public class WikipediaIntroSource implements ArtistIntroSource {

    private static final List<String> LANGUAGES = List.of("ko", "en");

    private final MusicBrainzClient musicBrainzClient;
    private final WikipediaClient wikipediaClient;

    @Override
    public IntroSourceType type() {
        return IntroSourceType.WIKIPEDIA;
    }

    @Override
    public Optional<ArtistIntro> fetch(String mbid) {
        Optional<String> wikidataId = musicBrainzClient.wikidataId(mbid);
        if (wikidataId.isEmpty()) {
            return Optional.empty();
        }
        for (String lang : LANGUAGES) {
            Optional<ArtistIntro> intro = wikipediaClient.articleTitle(wikidataId.get(), lang)
                    .flatMap(title -> wikipediaClient.summary(lang, title))
                    .map(s -> new ArtistIntro(s.extract(), s.url(), label(lang)));
            if (intro.isPresent()) {
                return intro;
            }
        }
        return Optional.empty();
    }

    private static String label(String lang) {
        return "ko".equals(lang) ? "위키백과" : "위키백과 (영어)";
    }
}
