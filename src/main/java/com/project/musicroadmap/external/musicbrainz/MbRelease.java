package com.project.musicroadmap.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** 발매판. media는 inc=recordings로 조회했을 때만 채워진다 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MbRelease(String id, String title, String date, String country, List<MbMedium> media) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MbMedium(int position, String format, List<MbTrack> tracks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MbTrack(String position, String title, Integer length, MbRecording recording) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MbRecording(String id) {
    }
}
