package com.project.musicroadmap.archive;

import com.project.musicroadmap.personal.PersonalDtos.GenreOverrideResponse;
import com.project.musicroadmap.personal.PersonalDtos.MyNotes;
import com.project.musicroadmap.preference.Preference;
import java.util.List;

public final class ArchiveDtos {

    private ArchiveDtos() {
    }

    /**
     * 가수 상세 (기획서 18장).
     * @param albumsAvailable MusicBrainz ID가 없으면 false (앨범 목록을 가져올 수 없음)
     * @param officialGenreId 공식 분류 (참조 레이어)
     * @param myGenre         내가 다르게 분류했으면 값이 있다 (기획서 15.2 규칙 4)
     * @param myNotes         내 감상과 태그
     */
    public record ArtistDetailResponse(
            Long id,
            String name,
            Preference myPreference,
            List<GenreRole> genreRoles,
            List<AlbumSummary> albums,
            boolean albumsAvailable,
            Long officialGenreId,
            GenreOverrideResponse myGenre,
            MyNotes myNotes,
            ExternalIntro externalIntro) {
    }

    /** 위키백과 등 외부 소개 글. 출처(label, url)를 반드시 함께 보여준다 (기획서 14.1) */
    public record ExternalIntro(String text, String sourceLabel, String sourceUrl) {
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
            List<AlbumTrackItem> tracks,
            MyNotes myNotes) {
    }

    /** myPreference: 이 곡에 남긴 좋아요/별로 (없으면 null) */
    public record AlbumTrackItem(Long id, int discNumber, int trackNumber, String title, Integer lengthMs,
                                 Preference myPreference) {
    }
}
