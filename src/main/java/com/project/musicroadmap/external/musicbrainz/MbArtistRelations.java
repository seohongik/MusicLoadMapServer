package com.project.musicroadmap.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** 아티스트 사이의 관계 (inc=artist-rels, 기획서 24장) */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MbArtistRelations(String id, String name, String type, List<Relation> relations) {

    /** direction: forward면 "나 → 상대" (예: 내가 그 밴드의 멤버), backward면 "상대 → 나" */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Relation(String type, String direction, RelatedArtist artist) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelatedArtist(String id, String name, String type, String disambiguation) {
    }

    public List<Relation> relationsOrEmpty() {
        return relations == null ? List.of() : relations;
    }
}
