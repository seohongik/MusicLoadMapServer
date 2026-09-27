package com.project.musicroadmap.genre.mapping;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 최다 득표. 동률이면 ID 작은 순 */
@Component
public class MostVotedMapping implements GenreMappingStrategy {

    @Override
    public GenreMappingType type() {
        return GenreMappingType.MOST_VOTED;
    }

    @Override
    public Optional<Long> choose(List<GenreCandidate> candidates, GenreGraph graph) {
        return candidates.stream()
                .min(Comparator.comparingInt(GenreCandidate::votes).reversed()
                        .thenComparingLong(GenreCandidate::genreId))
                .map(GenreCandidate::genreId);
    }
}
