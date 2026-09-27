package com.project.musicroadmap.genre.seed;

import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.DataSource;
import com.project.musicroadmap.genre.Genre;
import com.project.musicroadmap.genre.GenreArtist;
import com.project.musicroadmap.genre.GenreArtistRepository;
import com.project.musicroadmap.genre.GenreEdge;
import com.project.musicroadmap.genre.GenreEdgeRepository;
import com.project.musicroadmap.genre.GenreGraphProvider;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.genre.TrackRepository;
import com.project.musicroadmap.genre.TrackRole;
import com.project.musicroadmap.genre.mapping.GenreAlias;
import com.project.musicroadmap.genre.mapping.GenreAliasRepository;
import com.project.musicroadmap.genre.seed.SeedCatalog.GenreSeed;
import com.project.musicroadmap.genre.seed.SeedCatalog.TrackSeed;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 큐레이션 데이터 파일(JSON)을 DB에 맞춘다 (기획서 20장). 서버가 켜질 때마다 실행된다.
 * - 없는 것만 추가한다: 장르(code), 간선(from→to), 가수(MusicBrainz ID 또는 이름), 대표곡(장르·가수·제목), 장르-가수 소개, 별칭
 * - 이미 있는 큐레이션 장르·소개는 파일 값으로 맞춘다 (파일이 원본)
 * - 유저 데이터(가져온 가수의 장르, 기록, 선호)와 파일에서 빠진 데이터는 지우지 않는다
 * 그래서 새 데이터를 넣을 때는 파일만 고치면 된다. 빈 DB든 이미 쓰던 DB든 똑같이 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataLoader implements ApplicationRunner {

    private final GenreRepository genreRepository;
    private final GenreEdgeRepository edgeRepository;
    private final ArtistRepository artistRepository;
    private final TrackRepository trackRepository;
    private final GenreArtistRepository genreArtistRepository;
    private final GenreAliasRepository aliasRepository;
    private final GenreGraphProvider graphProvider;

    @Value("${app.seed.location:" + SeedData.MAIN + "}")
    private String location;

    /**
     * 주의: 같은 클래스의 sync()를 직접 부르면 sync()의 @Transactional은 적용되지 않는다(스프링 프록시를 거치지 않는 자기 호출).
     * 그래서 run()에도 트랜잭션을 건다. 트랜잭션이 없으면 저장한 뒤에 바꾼 값(ID 연결 등)이 DB에 반영되지 않는다.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        sync(SeedData.load(location));
    }

    /** 파일 내용을 DB에 맞춘다. 여러 번 실행해도 결과가 같다 (없는 것만 추가) */
    @Transactional
    public void sync(SeedCatalog catalog) {
        Counter counter = new Counter();

        Map<String, Genre> genres = syncGenres(catalog, counter);
        syncEdges(catalog, genres, counter);
        Map<String, Artist> artists = syncArtists(catalog, genres, counter);
        syncTracksAndIntros(catalog, genres, artists, counter);
        syncAliases(catalog, genres, counter);

        if (counter.genresChanged || counter.edges > 0) {
            graphProvider.reload();
        }
        log.info("큐레이션 데이터 반영 ({}개 장르 파일): 장르 +{}(갱신 {}), 간선 +{}, 가수 +{}, 대표곡 +{}, 소개 +{}(갱신 {}), 별칭 +{}(옮김 {}), MusicBrainz ID 연결 {}",
                catalog.genres().size(), counter.genres, counter.genresUpdated, counter.edges, counter.artists, counter.tracks,
                counter.intros, counter.introsUpdated, counter.aliases, counter.aliasesMoved, counter.mbidsLinked);
    }

    private Map<String, Genre> syncGenres(SeedCatalog catalog, Counter counter) {
        Map<String, Genre> byCode = new LinkedHashMap<>();
        genreRepository.findAll().forEach(g -> byCode.put(g.getCode(), g));
        for (GenreSeed g : catalog.genres()) {
            Genre existing = byCode.get(g.code());
            if (existing == null) {
                byCode.put(g.code(), genreRepository.save(Genre.curated(
                        g.code(), g.name(), g.nameKo(), g.originDecade(), g.description(), g.posX(), g.posY())));
                counter.genres++;
                counter.genresChanged = true;
            } else if (existing.syncFromSeed(g.name(), g.nameKo(), g.originDecade(), g.description(), g.posX(), g.posY())) {
                counter.genresUpdated++;
                counter.genresChanged = true;
            }
        }
        return byCode;
    }

    private void syncEdges(SeedCatalog catalog, Map<String, Genre> genres, Counter counter) {
        Set<String> existing = edgeRepository.findAll().stream()
                .map(e -> e.getFromGenre().getCode() + ">" + e.getToGenre().getCode())
                .collect(Collectors.toSet());
        for (SeedCatalog.EdgeSeed e : catalog.edges()) {
            if (existing.add(e.from() + ">" + e.to())) {
                edgeRepository.save(GenreEdge.of(genres.get(e.from()), genres.get(e.to())));
                counter.edges++;
            }
        }
    }

    /** 가수는 MusicBrainz ID로 먼저 찾고(가져온 가수와 겹치지 않게), 없으면 큐레이션 가수 이름으로 찾는다 */
    private Map<String, Artist> syncArtists(SeedCatalog catalog, Map<String, Genre> genres, Counter counter) {
        Map<String, Artist> byName = new HashMap<>();
        for (TrackSeed t : catalog.tracks()) {
            if (byName.containsKey(t.artist())) {
                continue;
            }
            Genre firstGenre = genres.get(t.genre());
            String mbid = catalog.artistMbids().get(t.artist());
            Optional<Artist> found = Optional.ofNullable(mbid).flatMap(artistRepository::findByMbid)
                    .or(() -> artistRepository.findFirstByNameIgnoreCaseAndSourceOrderByIdAsc(t.artist(), DataSource.CURATED));
            Artist artist = found.orElseGet(() -> {
                counter.artists++;
                return artistRepository.save(Artist.curated(t.artist(), firstGenre));
            });
            artist.assignPrimaryGenreIfMissing(firstGenre);
            if (mbid != null && artist.getMbid() == null && artistRepository.findByMbid(mbid).isEmpty()) {
                artist.linkMbid(mbid);
                counter.mbidsLinked++;
            }
            byName.put(t.artist(), artist);
        }
        return byName;
    }

    /** 대표곡과 "이 장르에서의 역할" 소개. 장르 안에서 적힌 순서가 표시 순서, 앞의 3곡이 대표곡 */
    private void syncTracksAndIntros(SeedCatalog catalog, Map<String, Genre> genres, Map<String, Artist> artists, Counter counter) {
        Set<String> existingTracks = new HashSet<>();
        trackRepository.findAll().forEach(t -> existingTracks.add(trackKey(t.getGenre().getId(), t.getArtist().getId(), t.getTitle())));
        Map<String, GenreArtist> existingIntros = new HashMap<>();
        genreArtistRepository.findAll().forEach(ga -> existingIntros.put(ga.getGenre().getId() + ":" + ga.getArtist().getId(), ga));

        Map<String, Integer> orderInGenre = new HashMap<>();
        for (TrackSeed t : catalog.tracks()) {
            Genre genre = genres.get(t.genre());
            Artist artist = artists.get(t.artist());
            int order = orderInGenre.merge(t.genre(), 1, Integer::sum) - 1;

            if (existingTracks.add(trackKey(genre.getId(), artist.getId(), t.title()))) {
                trackRepository.save(Track.curated(genre, artist, t.title(), t.year(), order < 3 ? TrackRole.MAIN : TrackRole.SPARE, order));
                counter.tracks++;
            }

            GenreArtist intro = existingIntros.get(genre.getId() + ":" + artist.getId());
            if (intro == null) {
                existingIntros.put(genre.getId() + ":" + artist.getId(),
                        genreArtistRepository.save(GenreArtist.of(genre, artist, t.intro(), order)));
                counter.intros++;
            } else if (intro.syncFromSeed(t.intro(), order)) {
                counter.introsUpdated++;
            }
        }
    }

    /** 별칭은 파일이 원본: 없으면 추가하고, 다른 장르를 가리키고 있으면 파일의 장르로 옮긴다 */
    private void syncAliases(SeedCatalog catalog, Map<String, Genre> genres, Counter counter) {
        Map<String, GenreAlias> existing = aliasRepository.findAll().stream()
                .collect(Collectors.toMap(GenreAlias::getAlias, a -> a));
        Set<String> handled = new HashSet<>();
        for (GenreSeed g : catalog.genres()) {
            Genre genre = genres.get(g.code());
            for (String alias : aliasesOf(catalog, g)) {
                String key = alias.toLowerCase();
                if (!handled.add(key)) {
                    continue; // 파일 안에서 먼저 나온 장르가 우선
                }
                GenreAlias row = existing.get(key);
                if (row == null) {
                    aliasRepository.save(GenreAlias.of(alias, genre));
                    counter.aliases++;
                } else if (row.pointTo(genre)) {
                    counter.aliasesMoved++;
                }
            }
        }
    }

    private static List<String> aliasesOf(SeedCatalog catalog, GenreSeed g) {
        List<String> all = new ArrayList<>(List.of(g.name()));
        all.addAll(catalog.aliases().getOrDefault(g.code(), List.of()));
        return all;
    }

    private static String trackKey(Long genreId, Long artistId, String title) {
        return genreId + ":" + artistId + ":" + title.toLowerCase();
    }

    private static class Counter {
        int genres;
        int genresUpdated;
        boolean genresChanged;
        int edges;
        int artists;
        int tracks;
        int intros;
        int introsUpdated;
        int aliases;
        int aliasesMoved;
        int mbidsLinked;
    }
}
