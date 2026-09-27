package com.project.musicroadmap.archive;

import com.project.musicroadmap.archive.ArchiveDtos.AlbumDetailResponse;
import com.project.musicroadmap.archive.ArchiveDtos.AlbumSummary;
import com.project.musicroadmap.archive.ArchiveDtos.AlbumTrackItem;
import com.project.musicroadmap.archive.ArchiveDtos.ArtistDetailResponse;
import com.project.musicroadmap.archive.ArchiveDtos.GenreRole;
import com.project.musicroadmap.common.AppClock;
import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.external.musicbrainz.MbRelease;
import com.project.musicroadmap.external.musicbrainz.MbReleaseGroup;
import com.project.musicroadmap.external.musicbrainz.MusicBrainzClient;
import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.GenreArtistRepository;
import com.project.musicroadmap.preference.TargetType;
import com.project.musicroadmap.preference.UserPreferences;
import com.project.musicroadmap.preference.PreferenceService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 가수 상세 → 앨범 → 수록곡 (기획서 18장). 처음 열 때 MusicBrainz에서 가져와 저장한다.
 * 외부 호출은 트랜잭션 밖에서, DB 작업만 짧은 트랜잭션으로 묶는다 (14.2와 같은 원칙).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    /** 앨범 목록 갱신 주기 */
    static final Duration ALBUM_REFRESH = Duration.ofDays(30);
    /** 앨범 목록 조회 최대 페이지 (100개씩). 라이브 앨범이 수백 개인 가수도 있어 상한을 둔다 */
    private static final int MAX_ALBUM_PAGES = 3;

    private final ArtistRepository artistRepository;
    private final GenreArtistRepository genreArtistRepository;
    private final AlbumRepository albumRepository;
    private final AlbumTrackRepository albumTrackRepository;
    private final PreferenceService preferenceService;
    private final MusicBrainzClient musicBrainzClient;
    private final TransactionTemplate transactionTemplate;
    private final AppClock clock;

    public ArtistDetailResponse getArtistDetail(Long userId, Long artistId) {
        Artist artist = inTx(() -> artistRepository.findById(artistId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TARGET_NOT_FOUND));

        if (artist.getMbid() != null && needsAlbumSync(artist)) {
            syncAlbums(artist);
        }

        return inTx(() -> {
            UserPreferences preferences = preferenceService.load(userId);
            List<GenreRole> roles = genreArtistRepository.findByArtistIdOrderByGenreIdAsc(artistId).stream()
                    .map(ga -> new GenreRole(ga.getGenre().getId(), ga.getIntro()))
                    .toList();
            List<AlbumSummary> albums = albumRepository.findByArtistIdOrderByReleaseYearAscIdAsc(artistId).stream()
                    .map(AlbumSummary::from)
                    .toList();
            return new ArtistDetailResponse(artist.getId(), artist.getName(),
                    preferences.of(TargetType.ARTIST, artistId), roles, albums, artist.getMbid() != null);
        });
    }

    public AlbumDetailResponse getAlbumDetail(Long userId, Long albumId) {
        Album album = inTx(() -> albumRepository.findWithArtistById(albumId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TARGET_NOT_FOUND));

        if (!album.tracksSynced()) {
            syncTracks(album);
        }

        return inTx(() -> {
            UserPreferences preferences = preferenceService.load(userId);
            List<AlbumTrackItem> tracks = albumTrackRepository.findByAlbumIdOrderByDiscNumberAscTrackNumberAsc(albumId).stream()
                    .map(t -> new AlbumTrackItem(t.getId(), t.getDiscNumber(), t.getTrackNumber(), t.getTitle(),
                            t.getLengthMs(), preferences.of(TargetType.ALBUM_TRACK, t.getId())))
                    .toList();
            return new AlbumDetailResponse(album.getId(), album.getArtist().getId(), album.getArtist().getName(),
                    album.getTitle(), album.getReleaseYear(), album.coverUrl(), tracks);
        });
    }

    // --- 앨범 목록 ---------------------------------------------------------------------------------

    private boolean needsAlbumSync(Artist artist) {
        LocalDateTime synced = artist.getAlbumsSyncedAt();
        return synced == null || synced.isBefore(clock.now().minus(ALBUM_REFRESH));
    }

    /** 정규 앨범만 남긴다 (라이브·컴필레이션·데모 등 보조 유형이 있으면 제외) */
    private void syncAlbums(Artist artist) {
        List<MbReleaseGroup> groups = musicBrainzClient.browseAlbumGroups(artist.getMbid(), MAX_ALBUM_PAGES).stream()
                .filter(MbReleaseGroup::isStudioAlbum)
                .toList();

        transactionTemplate.executeWithoutResult(status -> {
            Artist managed = artistRepository.findById(artist.getId()).orElseThrow();
            // 새 목록에서 빠진 앨범(예: 예전에 섞여 들어온 해적판)은 정리한다.
            // 단, 이미 열어서 수록곡이 있는 앨범은 곡 평가가 붙어 있을 수 있어 남긴다.
            Set<String> fresh = groups.stream().map(MbReleaseGroup::id).collect(Collectors.toSet());
            albumRepository.findByArtistIdOrderByReleaseYearAscIdAsc(artist.getId()).stream()
                    .filter(a -> !fresh.contains(a.getMbid()) && !a.tracksSynced())
                    .forEach(albumRepository::delete);
            for (MbReleaseGroup g : groups) {
                Integer year = yearOf(g.firstReleaseDate());
                albumRepository.findByMbid(g.id()).ifPresentOrElse(
                        existing -> existing.update(g.title(), year),
                        () -> albumRepository.save(Album.of(managed, g.id(), g.title(), year)));
            }
            managed.markAlbumsSynced(clock.now());
        });
        log.info("앨범 목록 동기화: {} ({}장)", artist.getName(), groups.size());
    }

    // --- 수록곡 -----------------------------------------------------------------------------------

    private void syncTracks(Album album) {
        List<MbRelease> releases = musicBrainzClient.browseReleases(album.getMbid(), true);
        if (releases.isEmpty()) {
            releases = musicBrainzClient.browseReleases(album.getMbid(), false);
        }
        Optional<MbRelease> chosen = chooseRelease(releases);
        if (chosen.isEmpty()) {
            transactionTemplate.executeWithoutResult(status ->
                    albumRepository.findById(album.getId()).orElseThrow().markTracksSynced(null, clock.now()));
            return;
        }
        MbRelease release = musicBrainzClient.getReleaseWithRecordings(chosen.get().id());

        transactionTemplate.executeWithoutResult(status -> {
            Album managed = albumRepository.findById(album.getId()).orElseThrow();
            if (managed.tracksSynced()) {
                return; // 다른 요청이 먼저 저장했다
            }
            List<AlbumTrack> tracks = new ArrayList<>();
            for (MbRelease.MbMedium medium : release.media() == null ? List.<MbRelease.MbMedium>of() : release.media()) {
                List<MbRelease.MbTrack> mediumTracks = medium.tracks() == null ? List.of() : medium.tracks();
                for (int i = 0; i < mediumTracks.size(); i++) {
                    MbRelease.MbTrack t = mediumTracks.get(i);
                    tracks.add(AlbumTrack.of(managed, medium.position(), i + 1, t.title(), t.length(),
                            t.recording() == null ? null : t.recording().id()));
                }
            }
            albumTrackRepository.saveAll(tracks);
            managed.markTracksSynced(release.id(), clock.now());
        });
    }

    /**
     * 가장 이른 발매판을 고른다. 날짜가 연도만 있는 발매판보다 정확한 날짜가 있는 원래 발매판을 우선한다.
     * (예: "2013"은 "2013-12-31"로 보고 비교 → "2013-09-09" 발매판이 먼저)
     */
    static Optional<MbRelease> chooseRelease(List<MbRelease> releases) {
        return releases.stream().min(Comparator.comparing((MbRelease r) -> sortableDate(r.date())));
    }

    static String sortableDate(String date) {
        if (date == null || date.isBlank()) {
            return "9999-12-31";
        }
        return switch (date.length()) {
            case 4 -> date + "-12-31";
            case 7 -> date + "-31";
            default -> date;
        };
    }

    static Integer yearOf(String date) {
        if (date == null || date.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private <T> T inTx(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }
}
