package com.project.musicroadmap.genre.importer;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.external.musicbrainz.MbArtist;
import com.project.musicroadmap.external.musicbrainz.MbGenre;
import com.project.musicroadmap.external.musicbrainz.MusicBrainzClient;
import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.genre.importer.ArtistImportDtos.ExternalArtistResponse;
import com.project.musicroadmap.genre.importer.ArtistImportDtos.ImportArtistResponse;
import com.project.musicroadmap.genre.mapping.GenreMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 우리 DB에 없는 아티스트를 MusicBrainz에서 가져온다 (기획서 14.2).
 * - 검색 결과는 저장하지 않는다. 유저가 고른 아티스트만 저장한다.
 * - 외부 호출(1초 이상 걸릴 수 있음)은 트랜잭션 밖에서 하고, DB 작업만 짧은 트랜잭션으로 묶는다.
 */
@Service
@RequiredArgsConstructor
public class ArtistImportService {

    private static final int SEARCH_LIMIT = 10;
    private static final int GENRE_PREVIEW = 5;

    private final MusicBrainzClient musicBrainzClient;
    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;
    private final TransactionTemplate transactionTemplate;

    public List<ExternalArtistResponse> searchExternal(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<MbArtist> found = musicBrainzClient.searchArtists(keyword, SEARCH_LIMIT);

        Map<String, Long> imported = transactionTemplate.execute(status ->
                artistRepository.findByMbidIn(found.stream().map(MbArtist::id).toList()).stream()
                        .collect(Collectors.toMap(Artist::getMbid, Artist::getId)));

        return found.stream()
                .map(a -> new ExternalArtistResponse(a.id(), a.name(), describe(a), imported.get(a.id())))
                .toList();
    }

    public ImportArtistResponse importArtist(String mbid) {
        Optional<ImportArtistResponse> existing = transactionTemplate.execute(status ->
                artistRepository.findByMbid(mbid).map(a -> toResponse(a, List.of())));
        if (existing != null && existing.isPresent()) {
            return existing.get();
        }

        MbArtist mb = musicBrainzClient.getArtist(mbid);
        Optional<Long> genreId = genreMapper.map(mb.genresOrEmpty());
        List<String> externalGenres = mb.genresOrEmpty().stream()
                .sorted(Comparator.comparingInt(MbGenre::count).reversed())
                .limit(GENRE_PREVIEW)
                .map(MbGenre::name)
                .toList();

        return transactionTemplate.execute(status -> {
            // 같은 이름의 큐레이션 아티스트가 있으면 새로 만들지 않고 외부 ID만 연결한다 (직접 다듬은 장르 유지)
            Artist artist = artistRepository.findFirstByNameIgnoreCaseAndMbidIsNull(mb.name())
                    .map(curated -> {
                        curated.linkMbid(mbid);
                        return curated;
                    })
                    .orElseGet(() -> artistRepository.save(Artist.imported(
                            mb.name(), mbid, genreId.map(genreRepository::getReferenceById).orElse(null))));
            return toResponse(artist, externalGenres);
        });
    }

    private ImportArtistResponse toResponse(Artist artist, List<String> externalGenres) {
        Long primaryGenreId = artist.getPrimaryGenre() == null ? null : artist.getPrimaryGenre().getId();
        return new ImportArtistResponse(artist.getId(), artist.getName(), primaryGenreId, artist.getSource(), externalGenres);
    }

    /** 동명이인 구분용 설명: "Group · GB · 설명" */
    private static String describe(MbArtist a) {
        return Stream.of(a.type(), a.country(), a.disambiguation())
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.joining(" · "));
    }
}
