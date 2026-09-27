package com.project.musicroadmap;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.GenreArtistRepository;
import com.project.musicroadmap.genre.GenreEdgeRepository;
import com.project.musicroadmap.genre.GenreGraphProvider;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.genre.TrackRepository;
import com.project.musicroadmap.genre.mapping.GenreAliasRepository;
import com.project.musicroadmap.genre.seed.SeedData;
import com.project.musicroadmap.genre.seed.SeedDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 기획서 20장: 이미 데이터가 있는 DB에 파일의 새 데이터만 추가되고, 여러 번 실행해도 중복이 생기지 않는다 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeedDataLoaderTest {

    @Autowired SeedDataLoader loader;
    @Autowired GenreRepository genreRepository;
    @Autowired GenreEdgeRepository edgeRepository;
    @Autowired ArtistRepository artistRepository;
    @Autowired TrackRepository trackRepository;
    @Autowired GenreArtistRepository genreArtistRepository;
    @Autowired GenreAliasRepository aliasRepository;
    @Autowired GenreGraphProvider graphProvider;

    @Test
    void 같은_파일로_다시_실행하면_아무것도_늘지_않는다() {
        long genres = genreRepository.count();
        long tracks = trackRepository.count();
        long intros = genreArtistRepository.count();

        loader.sync(SeedData.fixture());

        assertThat(genreRepository.count()).isEqualTo(genres);
        assertThat(trackRepository.count()).isEqualTo(tracks);
        assertThat(genreArtistRepository.count()).isEqualTo(intros);
    }

    @Test
    void 이미_쓰던_DB에_새_장르와_가수만_추가된다() {
        long genres = genreRepository.count();
        long edges = edgeRepository.count();
        long artists = artistRepository.count();
        long tracks = trackRepository.count();

        loader.sync(SeedData.load("seed/test-catalog-plus.json"));

        assertThat(genreRepository.count()).isEqualTo(genres + 1);
        assertThat(edgeRepository.count()).isEqualTo(edges + 1);
        assertThat(artistRepository.count()).as("기존 가수(Oasis)는 새로 만들지 않는다").isEqualTo(artists + 4);
        assertThat(trackRepository.count()).isEqualTo(tracks + 5);
        assertThat(aliasRepository.findAll()).anyMatch(a -> a.getAlias().equals("2 tone"));
        // 파일에서 다른 장르로 옮긴 별칭은 DB에서도 옮겨진다 (힙합 → 스카)
        assertThat(aliasRepository.findAll()).filteredOn(a -> a.getAlias().equals("rap"))
                .singleElement().satisfies(a -> assertThat(a.getGenre().getCode()).isEqualTo("SKA"));

        // 메모리 그래프도 새로 읽어서 새 장르로 탐색할 수 있다
        Long ska = genreRepository.findByCode("SKA").orElseThrow().getId();
        Long rnb = genreRepository.findByCode("RNB").orElseThrow().getId();
        assertThat(graphProvider.get().parents(ska)).containsExactly(rnb);
    }
}
