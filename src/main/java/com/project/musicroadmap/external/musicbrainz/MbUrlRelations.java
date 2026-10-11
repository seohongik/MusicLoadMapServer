package com.project.musicroadmap.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;

/** 아티스트의 외부 링크 (inc=url-rels). 위키데이터 링크로 위키백과 문서를 찾는다 (기획서 14.2 ⑤) */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MbUrlRelations(List<Relation> relations) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Relation(String type, Url url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Url(String resource) {
    }

    /** "https://www.wikidata.org/wiki/Q11649" → "Q11649" */
    public Optional<String> wikidataId() {
        if (relations == null) {
            return Optional.empty();
        }
        return relations.stream()
                .filter(r -> "wikidata".equals(r.type()) && r.url() != null && r.url().resource() != null)
                .map(r -> r.url().resource())
                .map(url -> url.substring(url.lastIndexOf('/') + 1))
                .filter(id -> id.matches("Q\\d+"))
                .findFirst();
    }
}
