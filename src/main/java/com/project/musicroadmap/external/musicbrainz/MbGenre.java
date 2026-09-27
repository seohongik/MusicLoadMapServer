package com.project.musicroadmap.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** count = 유저 투표 수 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MbGenre(String name, int count) {
}
