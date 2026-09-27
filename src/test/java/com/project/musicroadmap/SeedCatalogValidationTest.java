package com.project.musicroadmap;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.musicroadmap.genre.seed.SeedCatalog;
import com.project.musicroadmap.genre.seed.SeedCatalog.EdgeSeed;
import com.project.musicroadmap.genre.seed.SeedCatalog.GenreSeed;
import com.project.musicroadmap.genre.seed.SeedCatalog.TrackSeed;
import com.project.musicroadmap.genre.seed.SeedData;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 기획서 20장: 실제 큐레이션 데이터(seed/catalog.json)가 올바른지 검사한다.
 * 특정 값이 아니라 규칙만 보므로, 데이터를 추가해도 이 테스트는 고치지 않는다.
 */
class SeedCatalogValidationTest {

    private static final int MIN_ARTISTS_PER_GENRE = 4;
    private static final int NODE_WIDTH = 130;   // 앱 계보도 노드 크기 (dp)
    private static final int NODE_HEIGHT = 44;

    private final SeedCatalog catalog = SeedData.load(SeedData.MAIN);
    private final Set<String> codes = catalog.genres().stream().map(GenreSeed::code).collect(Collectors.toSet());

    @Test
    void 장르_코드는_겹치지_않고_설명이_있다() {
        assertThat(codes).hasSize(catalog.genres().size());
        assertThat(catalog.genres()).allSatisfy(g -> {
            assertThat(g.nameKo()).isNotBlank();
            assertThat(g.description()).isNotBlank();
            assertThat(g.originDecade()).isBetween(1900, 2020);
        });
    }

    @Test
    void 간선은_있는_장르끼리_한_번씩만_잇고_순환이_없다() {
        Set<String> seen = new HashSet<>();
        for (EdgeSeed e : catalog.edges()) {
            assertThat(codes).as("간선 " + e).contains(e.from(), e.to());
            assertThat(e.from()).as("자기 자신으로 가는 간선").isNotEqualTo(e.to());
            assertThat(seen.add(e.from() + ">" + e.to())).as("중복 간선 " + e).isTrue();
        }
        assertThat(hasCycle()).as("계보(기원 → 파생)에 순환이 있으면 안 된다").isFalse();
    }

    @Test
    void 모든_장르에_핵심_가수가_충분하고_소개가_있다() {
        Map<String, List<TrackSeed>> byGenre = catalog.tracks().stream().collect(Collectors.groupingBy(TrackSeed::genre));
        for (String code : codes) {
            List<TrackSeed> tracks = byGenre.getOrDefault(code, List.of());
            assertThat(tracks).as(code + " 가수 수").hasSizeGreaterThanOrEqualTo(MIN_ARTISTS_PER_GENRE);
            assertThat(tracks.stream().map(TrackSeed::artist).distinct().count())
                    .as(code + "에 같은 가수가 두 번 나오면 안 된다").isEqualTo(tracks.size());
        }
        assertThat(catalog.tracks()).allSatisfy(t -> {
            assertThat(codes).contains(t.genre());
            assertThat(t.intro()).as(t.artist() + " 소개").isNotBlank().hasSizeLessThanOrEqualTo(300);
            assertThat(t.year()).isBetween(1900, 2030);
        });
    }

    @Test
    void 모든_가수에_MusicBrainz_ID가_있고_겹치지_않는다() {
        Set<String> artists = catalog.tracks().stream().map(TrackSeed::artist).collect(Collectors.toSet());
        assertThat(catalog.artistMbids().keySet()).as("ID가 없는 가수").containsAll(artists);
        assertThat(catalog.artistMbids().values())
                .allMatch(id -> id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
                .doesNotHaveDuplicates();
    }

    @Test
    void 별칭은_있는_장르를_가리키고_겹치지_않는다() {
        assertThat(codes).containsAll(catalog.aliases().keySet());
        List<String> all = catalog.aliases().values().stream().flatMap(List::stream).map(String::toLowerCase).toList();
        assertThat(all).doesNotHaveDuplicates();
    }

    @Test
    void 계보도에서_장르_노드가_겹치지_않는다() {
        List<GenreSeed> genres = catalog.genres();
        for (int i = 0; i < genres.size(); i++) {
            for (int j = i + 1; j < genres.size(); j++) {
                GenreSeed a = genres.get(i);
                GenreSeed b = genres.get(j);
                boolean overlap = Math.abs(a.posX() - b.posX()) < NODE_WIDTH && Math.abs(a.posY() - b.posY()) < NODE_HEIGHT;
                assertThat(overlap).as(a.code() + " 와 " + b.code() + " 노드가 겹친다").isFalse();
            }
        }
    }

    private boolean hasCycle() {
        Map<String, List<String>> children = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        codes.forEach(c -> indegree.put(c, 0));
        for (EdgeSeed e : catalog.edges()) {
            children.computeIfAbsent(e.from(), k -> new java.util.ArrayList<>()).add(e.to());
            indegree.merge(e.to(), 1, Integer::sum);
        }
        Deque<String> queue = new ArrayDeque<>();
        indegree.forEach((c, d) -> { if (d == 0) queue.add(c); });
        int visited = 0;
        while (!queue.isEmpty()) {
            String c = queue.poll();
            visited++;
            for (String child : children.getOrDefault(c, List.of())) {
                if (indegree.merge(child, -1, Integer::sum) == 0) {
                    queue.add(child);
                }
            }
        }
        return visited != codes.size();
    }
}
