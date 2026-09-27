package com.project.musicroadmap.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MbReleaseGroupBrowseResponse(
        @JsonProperty("release-groups") List<MbReleaseGroup> releaseGroups,
        @JsonProperty("release-group-count") int count) {
}
