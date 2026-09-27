package com.project.musicroadmap.genre;

import com.project.musicroadmap.playback.PlaybackStrategy;
import com.project.musicroadmap.playback.PlaybackType;
import java.util.List;

/** GET /api/catalog 응답. 앱의 Catalog와 1:1 대응 */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    /**
     * @param playbackType 서버에 설정된 재생 방식 (NONE이면 모든 곡의 listenUrl이 null)
     * @param genreArtists 장르별 핵심 가수와 역할 소개 (기획서 17장)
     */
    public record CatalogResponse(
            PlaybackType playbackType,
            List<GenreDto> genres,
            List<EdgeDto> edges,
            List<TrackDto> tracks,
            List<ArtistDto> artists,
            List<GenreArtistDto> genreArtists) {
    }

    public record GenreDto(Long id, String code, String name, String nameKo, int originDecade,
                           String description, int posX, int posY) {
        static GenreDto from(Genre g) {
            return new GenreDto(g.getId(), g.getCode(), g.getName(), g.getNameKo(), g.getOriginDecade(),
                    g.getDescription(), g.getPosX(), g.getPosY());
        }
    }

    public record EdgeDto(Long fromGenreId, Long toGenreId, double baseCost) {
        static EdgeDto from(GenreEdge e) {
            return new EdgeDto(e.getFromGenre().getId(), e.getToGenre().getId(), e.getBaseCost());
        }
    }

    /** listenUrl: 재생 방식이 링크를 줄 때만 값이 있다 */
    public record TrackDto(Long id, Long genreId, Long artistId, String title, String artistName,
                           int releaseYear, TrackRole role, int displayOrder, String listenUrl) {
        static TrackDto from(Track t, PlaybackStrategy playback) {
            return new TrackDto(t.getId(), t.getGenre().getId(), t.getArtist().getId(), t.getTitle(),
                    t.getArtist().getName(), t.getReleaseYear(), t.getRole(), t.getDisplayOrder(),
                    playback.listenUrl(t).orElse(null));
        }
    }

    public record GenreArtistDto(Long genreId, Long artistId, String intro, int displayOrder) {
        static GenreArtistDto from(GenreArtist ga) {
            return new GenreArtistDto(ga.getGenre().getId(), ga.getArtist().getId(), ga.getIntro(), ga.getDisplayOrder());
        }
    }

    public record ArtistDto(Long id, String name, Long primaryGenreId) {
        static ArtistDto from(Artist a) {
            return new ArtistDto(a.getId(), a.getName(),
                    a.getPrimaryGenre() == null ? null : a.getPrimaryGenre().getId());
        }
    }
}
