package com.project.musicroadmap.genre.mapping;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 구체적인 장르 우선: 계보도에서 조상이 많은(더 아래에 있는) 장르. 동률이면 득표 → ID */
@Component
public class MostSpecificMapping implements GenreMappingStrategy {

    @Override
    public GenreMappingType type() {
        return GenreMappingType.MOST_SPECIFIC;
    }

    @Override
    public Optional<Long> choose(List<GenreCandidate> candidates, GenreGraph graph) {
        return candidates.stream()
                .min(Comparator.<GenreCandidate>comparingInt(c -> ancestorCount(graph, c.genreId())).reversed()
                        .thenComparing(Comparator.comparingInt(GenreCandidate::votes).reversed())
                        .thenComparingLong(GenreCandidate::genreId))
                .map(GenreCandidate::genreId);
    }

    static int ancestorCount(GenreGraph graph, long genreId) {
        Set<Long> seen = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>(graph.parents(genreId));
        while (!queue.isEmpty()) {
            long id = queue.poll();
            if (seen.add(id)) {
                queue.addAll(graph.parents(id));
            }
        }
        return seen.size();
    }
}
