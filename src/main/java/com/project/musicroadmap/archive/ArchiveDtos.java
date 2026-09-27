package com.project.musicroadmap.archive;

import com.project.musicroadmap.preference.Preference;
import java.util.List;

public final class ArchiveDtos {

    private ArchiveDtos() {
    }

    /**
     * 가수 상세 (기획서 18장).
     * @param albumsAvailable MusicBrainz ID가 없으면 false (앨범 목록을 가져올 수 없음)
     */
    public record ArtistDetailResponse(
            Long id,
            String name,
            Preference myPreference,
            List<GenreRole> genreRoles,
            List<AlbumSummary> albums,
            boolean albumsAvailable) {
    }

    /** 이 가수가 계보도에서 맡은 역할 (장르별 소개) */
    public record GenreRole(Long genreId, String intro) {
    }

    public record AlbumSummary(Long id, String title, Integer releaseYear, String coverUrl) {
        static AlbumSummary from(Album a) {
            return new AlbumSummary(a.getId(), a.getTitle(), a.getReleaseYear(), a.coverUrl());
        }
    }

    public record AlbumDetailResponse(
            Long id,
            Long artistId,
            String artistName,
            String title,
            Integer releaseYear,
            String coverUrl,
            List<AlbumTrackItem> tracks) {
    }

    /** myPreference: 이 곡에 남긴 좋아요/별로 (없으면 null) */
    public record AlbumTrackItem(Long id, int discNumber, int trackNumber, String title, Integer lengthMs,
                                 Preference myPreference) {
    }
}
