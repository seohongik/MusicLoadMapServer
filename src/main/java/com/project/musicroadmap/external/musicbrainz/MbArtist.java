package com.project.musicroadmap.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** MusicBrainz 아티스트 응답 중 쓰는 필드만. 나머지는 무시한다 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MbArtist(
        String id,
        String name,
        String type,
        String country,
        String disambiguation,
        Integer score,
        List<MbGenre> genres) {

    public List<MbGenre> genresOrEmpty() {
        return genres == null ? List.of() : genres;
    }
}
