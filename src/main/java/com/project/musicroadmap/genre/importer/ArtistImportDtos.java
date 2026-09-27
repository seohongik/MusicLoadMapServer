package com.project.musicroadmap.genre.importer;

import com.project.musicroadmap.genre.DataSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public final class ArtistImportDtos {

    private ArtistImportDtos() {
    }

    /**
     * MusicBrainz 검색 후보 (저장하지 않음).
     * @param importedArtistId 이미 우리 DB에 있으면 그 ID, 없으면 null
     */
    public record ExternalArtistResponse(String mbid, String name, String description, Long importedArtistId) {
    }

    public record ImportArtistRequest(
            @NotBlank
            @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "MusicBrainz ID 형식이 아니에요")
            String mbid) {
    }

    /**
     * @param primaryGenreId 계보도에 맞는 장르가 없으면 null → 앱에서 장르를 직접 고르게 한다
     * @param externalGenres MusicBrainz 장르 투표 상위 5개 (참고용)
     */
    public record ImportArtistResponse(Long artistId, String name, Long primaryGenreId, DataSource source,
                                       List<String> externalGenres) {
    }
}
