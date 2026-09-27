package com.project.musicroadmap.genre.mapping;

import com.project.musicroadmap.external.musicbrainz.MbGenre;
import com.project.musicroadmap.genre.GenreGraphProvider;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 장르 투표 → 대응표(GenreAlias)로 우리 장르 후보 만들기 → 설정된 전략으로 하나 고르기.
 * 전략은 app.genre-mapping.strategy 설정으로 바꾼다.
 */
@Component
public class GenreMapper {

    private final GenreAliasRepository aliasRepository;
    private final GenreGraphProvider graphProvider;
    private final Map<GenreMappingType, GenreMappingStrategy> strategies = new EnumMap<>(GenreMappingType.class);
    private final GenreMappingType defaultType;

    public GenreMapper(GenreAliasRepository aliasRepository, GenreGraphProvider graphProvider,
                       List<GenreMappingStrategy> strategies,
                       @Value("${app.genre-mapping.strategy:MOST_VOTED}") GenreMappingType defaultType) {
        this.aliasRepository = aliasRepository;
        this.graphProvider = graphProvider;
        strategies.forEach(s -> this.strategies.put(s.type(), s));
        this.defaultType = defaultType;
    }

    @Transactional(readOnly = true)
    public Optional<Long> map(List<MbGenre> externalGenres) {
        return map(externalGenres, defaultType);
    }

    @Transactional(readOnly = true)
    public Optional<Long> map(List<MbGenre> externalGenres, GenreMappingType type) {
        Map<String, Long> aliasToGenre = aliasRepository.findAll().stream()
                .collect(Collectors.toMap(GenreAlias::getAlias, a -> a.getGenre().getId()));
        return strategies.get(type).choose(toCandidates(externalGenres, aliasToGenre), graphProvider.get());
    }

    /** 같은 우리 장르로 대응되는 외부 장르들은 투표를 합친다 (예: hip hop + hip-hop) */
    public static List<GenreCandidate> toCandidates(List<MbGenre> externalGenres, Map<String, Long> aliasToGenre) {
        Map<Long, Integer> votes = new LinkedHashMap<>();
        for (MbGenre g : externalGenres) {
            Long genreId = aliasToGenre.get(g.name().toLowerCase());
            if (genreId != null) {
                votes.merge(genreId, g.count(), Integer::sum);
            }
        }
        return votes.entrySet().stream().map(e -> new GenreCandidate(e.getKey(), e.getValue())).toList();
    }
}
