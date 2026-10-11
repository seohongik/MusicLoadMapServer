package com.project.musicroadmap.roadmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.musicroadmap.auth.AuthService;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.roadmap.RoadmapDtos.CreateRoadmapRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.RoadmapResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.SharedRoadmapResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.SharedStep;
import com.project.musicroadmap.roadmap.RoadmapDtos.StepResponse;
import com.project.musicroadmap.roadmap.domain.StepStatus;
import com.project.musicroadmap.roadmap.domain.Verdict;
import com.project.musicroadmap.roadmap.exploration.ExplorationType;
import com.project.musicroadmap.roadmap.follow.FollowMode;
import com.project.musicroadmap.weight.UserWeights;
import com.project.musicroadmap.weight.WeightService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 기획서 21장: 공유와 따라가기. 같은 공유 로드맵이라도 따라가기 방식과 내 취향에 따라 경로가 다르다 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoadmapFollowTest {

    @Autowired RoadmapService roadmapService;
    @Autowired RoadmapShareService shareService;
    @Autowired AuthService authService;
    @Autowired GenreRepository genreRepository;
    @Autowired WeightService weightService;

    Long ownerId;
    Long followerId;
    String code;

    @BeforeEach
    void setUp() {
        ownerId = authService.devLogin("owner").getId();
        followerId = authService.devLogin("follower").getId();
        RoadmapResponse owned = roadmapService.create(ownerId, new CreateRoadmapRequest(
                ExplorationType.BRIDGE, genre("BRITPOP"), genre("HIP_HOP"), 5));
        roadmapService.next(ownerId, Verdict.LIKED);
        code = shareService.share(ownerId, owned.id()).shareCode();
    }

    private Long genre(String code) {
        return genreRepository.findByCode(code).orElseThrow().getId();
    }

    private List<Long> genres(String... codes) {
        return Arrays.stream(codes).map(this::genre).toList();
    }

    private static List<Long> activeGenres(RoadmapResponse r) {
        return r.steps().stream().filter(s -> s.status() != StepStatus.REPLACED).map(StepResponse::genreId).toList();
    }

    private void dislike(Long userId, String genreCode) {
        weightService.save(userId, new UserWeights(Map.of(genre(genreCode), UserWeights.DISLIKE_THRESHOLD)));
    }

    @Test
    void 공유하면_경로와_주인의_판정이_보인다() {
        SharedRoadmapResponse shared = shareService.get(followerId, code.toLowerCase());

        assertThat(shared.ownerNickname()).isEqualTo("owner");
        assertThat(shared.mine()).isFalse();
        assertThat(shared.steps().stream().map(SharedStep::genreId).toList())
                .isEqualTo(genres("BRITPOP", "BRITISH_INVASION", "SOUL", "FUNK", "HIP_HOP"));
        assertThat(shared.steps().get(1).verdict()).isEqualTo(Verdict.LIKED);
        assertThat(shared.steps().get(2).verdict()).isNull();
        assertThat(shareService.recent(followerId)).extracting(SharedRoadmapResponse::shareCode).contains(code);
    }

    @Test
    void 다시_공유해도_같은_코드() {
        Long roadmapId = roadmapService.current(ownerId).orElseThrow().id();
        assertThat(shareService.share(ownerId, roadmapId).shareCode()).isEqualTo(code);
    }

    @Test
    void 그대로_따라가기는_싫어하는_장르도_지나고_판정해도_경로가_그대로다() {
        dislike(followerId, "SOUL");

        RoadmapResponse r = roadmapService.follow(followerId, code, FollowMode.AS_IS);
        assertThat(activeGenres(r)).isEqualTo(genres("BRITPOP", "BRITISH_INVASION", "SOUL", "FUNK", "HIP_HOP"));
        assertThat(r.followMode()).isEqualTo(FollowMode.AS_IS);

        roadmapService.next(followerId, Verdict.DISLIKED);
        assertThat(activeGenres(roadmapService.current(followerId).orElseThrow()))
                .isEqualTo(genres("BRITPOP", "BRITISH_INVASION", "SOUL", "FUNK", "HIP_HOP"));
        assertThat(shareService.get(followerId, code).followerCount()).isEqualTo(1);
    }

    @Test
    void 내_취향대로_따라가기는_싫어하는_장르를_빼고_시작한다() {
        dislike(followerId, "SOUL");

        RoadmapResponse r = roadmapService.follow(followerId, code, FollowMode.ADAPT);
        assertThat(activeGenres(r)).isEqualTo(genres("BRITPOP", "BRITISH_INVASION", "FUNK", "HIP_HOP"));
    }

    @Test
    void 같은_공유_로드맵도_유저마다_결과가_다르다() {
        Long another = authService.devLogin("another").getId();
        dislike(followerId, "SOUL");

        RoadmapResponse mine = roadmapService.follow(followerId, code, FollowMode.ADAPT);
        RoadmapResponse theirs = roadmapService.follow(another, code, FollowMode.ADAPT);
        assertThat(activeGenres(mine)).isNotEqualTo(activeGenres(theirs));
    }

    @Test
    void 따라가기_실패_조건() {
        assertThatThrownBy(() -> roadmapService.follow(ownerId, code, FollowMode.AS_IS))
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_FOLLOW_OWN);
        assertThatThrownBy(() -> roadmapService.follow(followerId, "NOPE2345", FollowMode.AS_IS))
                .extracting("errorCode").isEqualTo(ErrorCode.SHARED_ROADMAP_NOT_FOUND);

        roadmapService.follow(followerId, code, FollowMode.AS_IS);
        assertThatThrownBy(() -> roadmapService.follow(followerId, code, FollowMode.AS_IS))
                .extracting("errorCode").isEqualTo(ErrorCode.ROADMAP_ALREADY_IN_PROGRESS);
    }

    @Test
    void 공유를_끄면_따라갈_수_없다() {
        shareService.unshare(ownerId, roadmapService.current(ownerId).orElseThrow().id());
        assertThatThrownBy(() -> roadmapService.follow(followerId, code, FollowMode.AS_IS))
                .extracting("errorCode").isEqualTo(ErrorCode.SHARED_ROADMAP_NOT_FOUND);
    }

    @Test
    void 싫어하는_장르를_빼면_남는_게_없을_때() {
        weightService.save(followerId, new UserWeights(Map.of(
                genre("BRITISH_INVASION"), 3.0, genre("SOUL"), 3.0, genre("FUNK"), 3.0, genre("HIP_HOP"), 3.0)));
        assertThatThrownBy(() -> roadmapService.follow(followerId, code, FollowMode.ADAPT))
                .extracting("errorCode").isEqualTo(ErrorCode.NOTHING_TO_FOLLOW);
    }
}
