package com.project.musicroadmap.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MbReleaseBrowseResponse(List<MbRelease> releases) {
}
