package com.project.musicroadmap.personal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.musicroadmap.auth.AuthService;
import com.project.musicroadmap.auth.UserService;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreGraphProvider;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.personal.UserGraphService.UserEdgeRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.CreateRoadmapRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.PreviewRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.PreviewResult;
import com.project.musicroadmap.roadmap.RoadmapDtos.StepResponse;
import com.project.musicroadmap.roadmap.RoadmapService;
import com.project.musicroadmap.roadmap.exploration.ExplorationType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 기획서 15.2 규칙 5: 내 연결은 켰을 때만 로드맵 계산에 들어가고, 공통 그래프는 바뀌지 않는다 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserEdgeTest {

    @Autowired UserGraphService userGraphService;
    @Autowired UserService userService;
    @Autowired RoadmapService roadmapService;
    @Autowired AuthService authService;
    @Autowired GenreRepository genreRepository;
    @Autowired GenreGraphProvider graphProvider;

    Long me;

    @BeforeEach
    void setUp() {
        me = authService.devLogin("me").getId();
    }

    private Long genre(String code) {
        return genreRepository.findByCode(code).orElseThrow().getId();
    }

    private List<Long> bridgePath() {
        return roadmapService.preview(me, new PreviewRequest(genre("BRITPOP"), genre("HIP_HOP"), 5)).stream()
                .filter(r -> r.explorationType() == ExplorationType.BRIDGE)
                .findFirst().map(PreviewResult::path).orElseThrow();
    }

    @Test
    void 내_연결은_켰을_때만_계산에_들어간다() {
        userGraphService.add(me, new UserEdgeRequest(genre("BRITPOP"), genre("HIP_HOP"), "둘 다 거리의 음악"));

        assertThat(bridgePath()).hasSizeGreaterThan(2);

        userService.changeUseMyEdges(me, true);
        assertThat(bridgePath()).containsExactly(genre("BRITPOP"), genre("HIP_HOP"));

        List<Long> created = roadmapService.create(me, new CreateRoadmapRequest(
                ExplorationType.BRIDGE, genre("BRITPOP"), genre("HIP_HOP"), 5)).steps().stream()
                .map(StepResponse::genreId).toList();
        assertThat(created).containsExactly(genre("BRITPOP"), genre("HIP_HOP"));
    }

    @Test
    void 공통_그래프는_바뀌지_않고_다른_유저에게도_영향이_없다() {
        Long other = authService.devLogin("other").getId();
        userGraphService.add(me, new UserEdgeRequest(genre("BRITPOP"), genre("HIP_HOP"), null));
        userService.changeUseMyEdges(me, true);
        userService.changeUseMyEdges(other, true);

        assertThat(graphProvider.get().connected(genre("BRITPOP"), genre("HIP_HOP"))).isFalse();
        assertThat(userGraphService.graphFor(other).connected(genre("BRITPOP"), genre("HIP_HOP"))).isFalse();
        assertThat(userGraphService.graphFor(me).connected(genre("BRITPOP"), genre("HIP_HOP"))).isTrue();
    }

    @Test
    void 계보의_의미를_깨는_연결은_막는다() {
        assertThatThrownBy(() -> userGraphService.add(me, new UserEdgeRequest(genre("SOUL"), genre("SOUL"), null)))
                .extracting("errorCode").isEqualTo(ErrorCode.USER_EDGE_SAME_GENRE);
        assertThatThrownBy(() -> userGraphService.add(me, new UserEdgeRequest(genre("BRITPOP"), genre("BRITISH_INVASION"), null)))
                .extracting("errorCode").isEqualTo(ErrorCode.USER_EDGE_EXISTS);
        // 힙합은 R&B의 후손이라 힙합 → R&B는 제자리로 돌아오는 연결
        assertThatThrownBy(() -> userGraphService.add(me, new UserEdgeRequest(genre("HIP_HOP"), genre("RNB"), null)))
                .extracting("errorCode").isEqualTo(ErrorCode.USER_EDGE_CYCLE);

        userGraphService.add(me, new UserEdgeRequest(genre("BRITPOP"), genre("HIP_HOP"), null));
        assertThatThrownBy(() -> userGraphService.add(me, new UserEdgeRequest(genre("HIP_HOP"), genre("BRITPOP"), null)))
                .extracting("errorCode").isEqualTo(ErrorCode.USER_EDGE_EXISTS);
    }

    @Test
    void 지우면_계산에서도_빠진다() {
        userGraphService.add(me, new UserEdgeRequest(genre("BRITPOP"), genre("HIP_HOP"), null));
        userService.changeUseMyEdges(me, true);
        userGraphService.delete(me, genre("BRITPOP"), genre("HIP_HOP"));
        assertThat(userGraphService.edges(me)).isEmpty();
        assertThat(bridgePath()).hasSizeGreaterThan(2);
    }
}
