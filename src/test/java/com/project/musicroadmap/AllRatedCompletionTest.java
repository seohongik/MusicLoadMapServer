package com.project.musicroadmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.musicroadmap.auth.AuthService;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.listening.Feedback;
import com.project.musicroadmap.roadmap.RoadmapDtos.CreateRoadmapRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.FeedbackResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.ListenRequest;
import com.project.musicroadmap.roadmap.RoadmapService;
import com.project.musicroadmap.roadmap.domain.Verdict;
import com.project.musicroadmap.roadmap.exploration.ExplorationType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 기획서 19장: 완료 방식을 ALL_RATED(초기 방식)로 바꾸면 모두 반응했을 때 자동 판정, 버튼은 막힌다 */
@SpringBootTest(properties = "app.step-completion.strategy=ALL_RATED")
@ActiveProfiles("test")
@Transactional
class AllRatedCompletionTest {

    @Autowired RoadmapService roadmapService;
    @Autowired AuthService authService;
    @Autowired GenreRepository genreRepository;

    @Test
    void 모든_곡에_반응하면_자동으로_판정하고_다음_장르로_버튼은_막힌다() {
        Long userId = authService.devLogin("all-rated").getId();
        Long britpop = genreRepository.findByCode("BRITPOP").orElseThrow().getId();
        Long hipHop = genreRepository.findByCode("HIP_HOP").orElseThrow().getId();
        roadmapService.create(userId, new CreateRoadmapRequest(ExplorationType.BRIDGE, britpop, hipHop, 5));

        assertThatThrownBy(() -> roadmapService.next(userId, null))
                .extracting("errorCode").isEqualTo(ErrorCode.STEP_COMPLETION_NOT_ALLOWED);

        List<Long> tracks = roadmapService.today(userId).orElseThrow().trackIds();
        FeedbackResponse last = null;
        for (Long trackId : tracks) {
            last = roadmapService.submitFeedback(userId, new ListenRequest(trackId, Feedback.LIKE, null, ""));
        }
        assertThat(last.genreVerdict()).isEqualTo(Verdict.LIKED);
        assertThat(roadmapService.current(userId).orElseThrow().currentStepIndex()).isEqualTo(2);
    }
}
