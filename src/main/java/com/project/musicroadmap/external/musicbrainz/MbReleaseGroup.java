package com.project.musicroadmap.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** 앨범 단위 (여러 발매판을 묶은 것). secondaryTypes가 비어 있으면 정규 앨범 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MbReleaseGroup(
        String id,
        String title,
        @JsonProperty("primary-type") String primaryType,
        @JsonProperty("secondary-types") List<String> secondaryTypes,
        @JsonProperty("first-release-date") String firstReleaseDate) {

    public boolean isStudioAlbum() {
        return "Album".equals(primaryType) && (secondaryTypes == null || secondaryTypes.isEmpty());
    }
}
