package com.project.musicroadmap.genre.mapping;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.List;
import java.util.Optional;

/** 외부 장르 투표 목록에서 계보도의 장르 하나를 고른다 */
public interface GenreMappingStrategy {

    GenreMappingType type();

    /** candidates가 비어 있으면 empty (계보도에 맞는 장르 없음) */
    Optional<Long> choose(List<GenreCandidate> candidates, GenreGraph graph);
}
