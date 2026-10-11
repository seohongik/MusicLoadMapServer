package com.project.musicroadmap.genre.intro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.musicroadmap.archive.ArchiveDtos.ArtistDetailResponse;
import com.project.musicroadmap.archive.ArchiveService;
import com.project.musicroadmap.auth.AuthService;
import com.project.musicroadmap.external.musicbrainz.MbUrlRelations;
import com.project.musicroadmap.external.musicbrainz.MusicBrainzClient;
import com.project.musicroadmap.external.wikipedia.WikipediaClient;
import com.project.musicroadmap.external.wikipedia.WikipediaClient.Summary;
import com.project.musicroadmap.genre.ArtistRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** 기획서 14.2 ⑤: MusicBrainz 링크 → 위키데이터 → 위키백과 요약 (한국어 우선) */
class ArtistIntroTest {

    @Test
    void 위키데이터_링크에서_ID를_뽑는다() {
        MbUrlRelations relations = new MbUrlRelations(List.of(
                new MbUrlRelations.Relation("official homepage", new MbUrlRelations.Url("https://oasisinet.com")),
                new MbUrlRelations.Relation("wikidata", new MbUrlRelations.Url("https://www.wikidata.org/wiki/Q19837"))));
        assertThat(relations.wikidataId()).contains("Q19837");
        assertThat(new MbUrlRelations(null).wikidataId()).isEmpty();
    }

    @Test
    void 한국어_문서가_없으면_영어() {
        MusicBrainzClient mb = mock(MusicBrainzClient.class);
        WikipediaClient wiki = mock(WikipediaClient.class);
        when(mb.wikidataId("m1")).thenReturn(Optional.of("Q1"));
        when(wiki.articleTitle("Q1", "ko")).thenReturn(Optional.empty());
        when(wiki.articleTitle("Q1", "en")).thenReturn(Optional.of("Oasis (band)"));
        when(wiki.summary("en", "Oasis (band)")).thenReturn(Optional.of(new Summary("English rock band", "https://en.wikipedia.org/wiki/Oasis_(band)")));

        ArtistIntro intro = new WikipediaIntroSource(mb, wiki).fetch("m1").orElseThrow();
        assertThat(intro.sourceLabel()).isEqualTo("위키백과 (영어)");
        assertThat(intro.text()).isEqualTo("English rock band");
    }

    @Test
    void 위키데이터_링크가_없으면_빈_값() {
        MusicBrainzClient mb = mock(MusicBrainzClient.class);
        when(mb.wikidataId(anyString())).thenReturn(Optional.empty());
        assertThat(new WikipediaIntroSource(mb, mock(WikipediaClient.class)).fetch("m1")).isEmpty();
    }

    @Nested
    @SpringBootTest(properties = "app.intro.source=WIKIPEDIA")
    @ActiveProfiles("test")
    @Transactional
    class 가수_상세 {

        @Autowired ArchiveService archiveService;
        @Autowired AuthService authService;
        @Autowired ArtistRepository artistRepository;
        @MockitoBean MusicBrainzClient musicBrainzClient;
        @MockitoBean WikipediaClient wikipediaClient;

        @Test
        void 처음_열_때_한국어_소개를_가져와_출처와_함께_저장하고_다음엔_부르지_않는다() {
            Long userId = authService.devLogin("me").getId();
            Long oasis = artistRepository.findByName("Oasis").orElseThrow().getId();
            when(musicBrainzClient.wikidataId(anyString())).thenReturn(Optional.of("Q19837"));
            when(wikipediaClient.articleTitle("Q19837", "ko")).thenReturn(Optional.of("오아시스 (밴드)"));
            when(wikipediaClient.summary("ko", "오아시스 (밴드)"))
                    .thenReturn(Optional.of(new Summary("오아시스는 영국의 록 밴드이다.", "https://ko.wikipedia.org/wiki/x")));

            ArtistDetailResponse first = archiveService.getArtistDetail(userId, oasis);
            archiveService.getArtistDetail(userId, oasis);

            assertThat(first.externalIntro().text()).isEqualTo("오아시스는 영국의 록 밴드이다.");
            assertThat(first.externalIntro().sourceLabel()).isEqualTo("위키백과");
            verify(musicBrainzClient, times(1)).wikidataId(anyString());
        }
    }
}
