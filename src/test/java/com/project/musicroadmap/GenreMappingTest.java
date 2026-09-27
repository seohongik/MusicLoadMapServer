package com.project.musicroadmap;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.musicroadmap.external.musicbrainz.MbGenre;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.genre.mapping.GenreCandidate;
import com.project.musicroadmap.genre.mapping.GenreMapper;
import com.project.musicroadmap.genre.mapping.MostSpecificMapping;
import com.project.musicroadmap.genre.mapping.MostVotedMapping;
import com.project.musicroadmap.genre.seed.SeedCatalog;
import com.project.musicroadmap.genre.seed.SeedData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 기획서 14.2.1 장르 매핑 전략. 투표 데이터는 실제 MusicBrainz 응답(Arctic Monkeys)을 옮긴 것 */
class GenreMappingTest {

    /** 고정 데이터(28장르). 실제 데이터가 늘어나도 기대값이 흔들리지 않는다 */
    private static final SeedCatalog FIXTURE = SeedData.fixture();

    private final GenreGraph graph = FIXTURE.graph();
    private final Map<String, Long> id = FIXTURE.codeToId();
    private final Map<String, Long> aliases = FIXTURE.aliasToGenreId();

    private final List<MbGenre> arcticMonkeys = List.of(
            new MbGenre("indie rock", 19), new MbGenre("alternative rock", 9),
            new MbGenre("garage rock revival", 7), new MbGenre("post-punk revival", 7),
            new MbGenre("baroque pop", 3), new MbGenre("psychedelic rock", 2), new MbGenre("rock", 2));

    private List<GenreCandidate> candidates(List<MbGenre> genres) {
        return GenreMapper.toCandidates(genres, aliases);
    }

    @Test
    void 같은_우리_장르로_모이는_외부_장르는_투표를_합친다() {
        // indie rock 19 + garage rock revival 7 + post-punk revival 7 → 인디 록 33
        assertThat(candidates(arcticMonkeys)).contains(new GenreCandidate(id.get("INDIE"), 33));
    }

    @Test
    void 최다_득표는_인디_록() {
        assertThat(new MostVotedMapping().choose(candidates(arcticMonkeys), graph)).contains(id.get("INDIE"));
    }

    @Test
    void 구체적인_장르_우선은_얼터너티브_록() {
        // 얼터너티브 록은 인디 록의 파생이라 조상이 더 많다 → 더 구체적
        assertThat(new MostSpecificMapping().choose(candidates(arcticMonkeys), graph)).contains(id.get("ALT_ROCK"));
    }

    @Test
    void 계보도에_맞는_장르가_없으면_비어_있다() {
        List<MbGenre> kpop = List.of(new MbGenre("k-pop", 10), new MbGenre("dance-pop", 4), new MbGenre("pop", 3));
        assertThat(new MostVotedMapping().choose(candidates(kpop), graph)).isEmpty();
    }

    @Test
    void 대소문자와_하이픈_별칭() {
        assertThat(candidates(List.of(new MbGenre("Hip-Hop", 5)))).containsExactly(new GenreCandidate(id.get("HIP_HOP"), 5));
    }
}
