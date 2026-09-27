package com.project.musicroadmap.genre.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.musicroadmap.genre.GenreGraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 큐레이션 데이터 파일(seed/catalog.json)의 내용 (기획서 20장).
 * 장르 순서가 곧 빈 DB에서의 ID 순서다. 동률 처리(ID 작은 순)가 흔들리지 않도록 새 장르는 뒤에 붙인다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedCatalog(
        List<GenreSeed> genres,
        List<EdgeSeed> edges,
        List<TrackSeed> tracks,
        Map<String, List<String>> aliases,
        Map<String, String> artistMbids) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GenreSeed(String code, String name, String nameKo, int originDecade,
                            String description, int posX, int posY) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EdgeSeed(String from, String to) {
    }

    /**
     * 장르 안에서 적힌 순서대로 앞의 3곡이 대표곡, 나머지는 예비곡.
     * intro = 이 장르에서 이 가수의 역할 한 줄 소개 (기획서 17장)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrackSeed(String genre, String title, String artist, int year, String intro) {
    }

    /** DB 없이 테스트할 때 쓰는 ID: 빈 DB에 순서대로 넣었을 때와 같은 ID(1부터) */
    public Map<String, Long> codeToId() {
        Map<String, Long> ids = new HashMap<>();
        for (int i = 0; i < genres.size(); i++) {
            ids.put(genres.get(i).code(), i + 1L);
        }
        return ids;
    }

    public GenreGraph graph() {
        Map<String, Long> ids = codeToId();
        List<GenreGraph.Node> nodes = new ArrayList<>();
        for (GenreSeed g : genres) {
            nodes.add(new GenreGraph.Node(ids.get(g.code()), g.nameKo(), g.originDecade()));
        }
        List<GenreGraph.Edge> edgeList = edges.stream()
                .map(e -> new GenreGraph.Edge(ids.get(e.from()), ids.get(e.to()), 1.0))
                .toList();
        return new GenreGraph(nodes, edgeList);
    }

    /** 장르 영문 이름(소문자) + 추가 별칭 → 장르 ID (SeedDataLoader와 같은 규칙) */
    public Map<String, Long> aliasToGenreId() {
        Map<String, Long> ids = codeToId();
        Map<String, Long> result = new HashMap<>();
        for (GenreSeed g : genres) {
            result.putIfAbsent(g.name().toLowerCase(), ids.get(g.code()));
            for (String alias : aliases.getOrDefault(g.code(), List.of())) {
                result.putIfAbsent(alias.toLowerCase(), ids.get(g.code()));
            }
        }
        return result;
    }
}
