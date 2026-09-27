package com.project.musicroadmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.genre.seed.SeedCatalog;
import com.project.musicroadmap.genre.seed.SeedData;
import com.project.musicroadmap.roadmap.exploration.BridgeStrategy;
import com.project.musicroadmap.roadmap.exploration.ChronologicalStrategy;
import com.project.musicroadmap.roadmap.exploration.DescendantsStrategy;
import com.project.musicroadmap.roadmap.exploration.ExplorationRequest;
import com.project.musicroadmap.roadmap.exploration.ExplorationStrategyResolver;
import com.project.musicroadmap.roadmap.exploration.ExplorationType;
import com.project.musicroadmap.roadmap.exploration.NeighborsStrategy;
import com.project.musicroadmap.roadmap.exploration.RootsStrategy;
import com.project.musicroadmap.weight.UserWeights;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 스프링 없이 순수 자바로 전략을 검증한다 (기획서 6장 시나리오, 10장 테스트) */
class ExplorationStrategyTest {

    /** 고정 데이터(28장르). 실제 데이터가 늘어나도 기대값이 흔들리지 않는다 */
    private static final SeedCatalog FIXTURE = SeedData.fixture();

    private final GenreGraph graph = FIXTURE.graph();
    private final Map<String, Long> id = FIXTURE.codeToId();
    private final ExplorationStrategyResolver resolver = new ExplorationStrategyResolver(
            List.of(new RootsStrategy(), new DescendantsStrategy(), new BridgeStrategy(),
                    new NeighborsStrategy(), new ChronologicalStrategy()));

    private List<Long> ids(String... codes) {
        return Arrays.stream(codes).map(id::get).toList();
    }

    private List<Long> explore(ExplorationType type, ExplorationRequest request, UserWeights weights) {
        return resolver.get(type).explore(graph, weights, request);
    }

    @Test
    void A_브릿팝에서_뿌리_찾기() {
        assertThat(explore(ExplorationType.ROOTS, new ExplorationRequest(id.get("BRITPOP"), null, 5), new UserWeights()))
                .isEqualTo(ids("BRITPOP", "BRITISH_INVASION", "ROCKABILLY", "COUNTRY"));
    }

    @Test
    void 뿌리_찾기_maxSteps_제한() {
        assertThat(explore(ExplorationType.ROOTS, new ExplorationRequest(id.get("BRITPOP"), null, 2), new UserWeights()))
                .isEqualTo(ids("BRITPOP", "BRITISH_INVASION", "ROCKABILLY"));
    }

    @Test
    void 블루스는_더_올라갈_뿌리가_없다() {
        assertThatThrownBy(() -> explore(ExplorationType.ROOTS, new ExplorationRequest(id.get("BLUES"), null, 5), new UserWeights()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DEAD_END);
    }

    @Test
    void C_브릿팝에서_힙합까지_잇기() {
        assertThat(explore(ExplorationType.BRIDGE, new ExplorationRequest(id.get("BRITPOP"), id.get("HIP_HOP"), 5), new UserWeights()))
                .isEqualTo(ids("BRITPOP", "BRITISH_INVASION", "SOUL", "FUNK", "HIP_HOP"));
    }

    @Test
    void D_소울을_싫어하면_다른_길로_잇는다() {
        UserWeights weights = new UserWeights();
        weights.applyDislike(id.get("SOUL"), graph);

        assertThat(explore(ExplorationType.BRIDGE, new ExplorationRequest(id.get("BRITPOP"), id.get("HIP_HOP"), 5), weights))
                .isEqualTo(ids("BRITPOP", "BRITISH_INVASION", "ROCKABILLY", "RNB", "DOO_WOP", "FUNK", "HIP_HOP"));
    }

    @Test
    void 싫어하면_파생_장르도_1_5배() {
        UserWeights weights = new UserWeights();
        weights.applyDislike(id.get("SOUL"), graph);

        assertThat(weights.of(id.get("SOUL"))).isEqualTo(3.0);
        assertThat(weights.of(id.get("PSYCHEDELIC"))).isEqualTo(1.5);
        assertThat(weights.of(id.get("FUNK"))).isEqualTo(1.5);
        assertThat(weights.of(id.get("RNB"))).isEqualTo(1.0);
    }

    @Test
    void 가중치는_상한_10() {
        UserWeights weights = new UserWeights();
        for (int i = 0; i < 5; i++) {
            weights.applyDislike(id.get("SOUL"), graph);
        }
        assertThat(weights.of(id.get("SOUL"))).isEqualTo(10.0);
    }

    @Test
    void 출발과_목표가_같으면_에러() {
        assertThatThrownBy(() -> explore(ExplorationType.BRIDGE,
                new ExplorationRequest(id.get("BRITPOP"), id.get("BRITPOP"), 5), new UserWeights()))
                .extracting("errorCode").isEqualTo(ErrorCode.SAME_GENRE);
    }

    @Test
    void 모든_탐험_방식에_구현체가_있다() {
        for (ExplorationType type : ExplorationType.values()) {
            assertThat(resolver.get(type).type()).isEqualTo(type);
        }
    }

    // --- Phase 2: 이웃 탐색, 시대순 여행 ---------------------------------------------------------------

    @Test
    void 이웃_탐색_브릿팝의_형제_장르를_등장_시기_순으로() {
        // 브릿팝의 부모(브릿 인베이전·글램·인디)의 다른 자식들
        assertThat(explore(ExplorationType.NEIGHBORS, new ExplorationRequest(id.get("BRITPOP"), null, 5), new UserWeights()))
                .isEqualTo(ids("BRITPOP", "SOUL", "FOLK_ROCK", "POP_ROCK", "HARD_ROCK", "PUNK"));
    }

    @Test
    void 이웃_탐색_싫어하는_장르는_빼고_다음_형제로() {
        UserWeights weights = new UserWeights();
        weights.applyDislike(id.get("SOUL"), graph);
        assertThat(explore(ExplorationType.NEIGHBORS, new ExplorationRequest(id.get("BRITPOP"), null, 5), weights))
                .isEqualTo(ids("BRITPOP", "FOLK_ROCK", "POP_ROCK", "HARD_ROCK", "PUNK", "ALT_ROCK"));
    }

    @Test
    void 이웃_탐색_형제가_없으면_더_갈_곳이_없다() {
        // 포스트 브릿팝의 부모는 브릿팝 하나, 브릿팝의 자식도 포스트 브릿팝 하나
        assertThatThrownBy(() -> explore(ExplorationType.NEIGHBORS, new ExplorationRequest(id.get("POST_BRITPOP"), null, 5), new UserWeights()))
                .extracting("errorCode").isEqualTo(ErrorCode.DEAD_END);
    }

    @Test
    void 시대순_여행_블루스에서_연대순으로() {
        assertThat(explore(ExplorationType.CHRONOLOGICAL, new ExplorationRequest(id.get("BLUES"), null, 5), new UserWeights()))
                .isEqualTo(ids("BLUES", "JAZZ", "RNB", "DOO_WOP", "ROCKABILLY", "BRITISH_INVASION"));
    }

    @Test
    void 시대순_여행_부모를_모두_거친_뒤에만_자식이_나온다() {
        List<Long> path = explore(ExplorationType.CHRONOLOGICAL, new ExplorationRequest(id.get("BLUES"), null, 8), new UserWeights());
        // 소울은 부모(R&B, 두왑, 브릿 인베이전)가 모두 나온 뒤에
        assertThat(path.indexOf(id.get("SOUL")))
                .isGreaterThan(path.indexOf(id.get("RNB")))
                .isGreaterThan(path.indexOf(id.get("DOO_WOP")))
                .isGreaterThan(path.indexOf(id.get("BRITISH_INVASION")));
    }

    @Test
    void 후손_찾기는_막다른_장르에서_지나온_장르로_돌아가_다른_갈래로_이어_간다() {
        // 브릿 인베이전 → 소울 → 사이키델릭 → 프로그레시브 록(자식 없음)
        // → 사이키델릭으로 돌아가 → 하드 록 → 헤비메탈  (예전에는 프로그레시브 록에서 3스텝으로 끝났다)
        assertThat(explore(ExplorationType.DESCENDANTS, new ExplorationRequest(id.get("BRITISH_INVASION"), null, 5), new UserWeights()))
                .isEqualTo(ids("BRITISH_INVASION", "SOUL", "PSYCHEDELIC", "PROG", "HARD_ROCK", "HEAVY_METAL"));
    }

    @Test
    void 뿌리_찾기는_돌아가지_않고_뿌리에서_끝난다() {
        assertThat(explore(ExplorationType.ROOTS, new ExplorationRequest(id.get("BRITPOP"), null, 8), new UserWeights()))
                .isEqualTo(ids("BRITPOP", "BRITISH_INVASION", "ROCKABILLY", "COUNTRY"));
    }
}

