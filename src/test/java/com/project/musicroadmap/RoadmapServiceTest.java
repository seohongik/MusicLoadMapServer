package com.project.musicroadmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.musicroadmap.auth.AuthService;
import com.project.musicroadmap.common.AppClock;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.genre.TrackRepository;
import com.project.musicroadmap.listening.DislikeReason;
import com.project.musicroadmap.listening.Feedback;
import com.project.musicroadmap.listening.ListeningLogRepository;
import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.PreferenceService;
import com.project.musicroadmap.preference.TargetType;
import com.project.musicroadmap.roadmap.RoadmapDtos.CreateRoadmapRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.FeedbackResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.ListenRequest;
import com.project.musicroadmap.roadmap.RoadmapDtos.RoadmapResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.StepResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.StepResultResponse;
import com.project.musicroadmap.roadmap.RoadmapDtos.TodayResponse;
import com.project.musicroadmap.weight.WeightService;
import com.project.musicroadmap.roadmap.RoadmapService;
import com.project.musicroadmap.roadmap.domain.StepStatus;
import com.project.musicroadmap.roadmap.domain.Verdict;
import com.project.musicroadmap.roadmap.exploration.ExplorationType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** H2 + 시드 데이터로 로드맵 흐름 전체를 검증한다 (기획서 5.5, 16.6) */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoadmapServiceTest {

    @Autowired RoadmapService roadmapService;
    @Autowired PreferenceService preferenceService;
    @Autowired AuthService authService;
    @Autowired GenreRepository genreRepository;
    @Autowired TrackRepository trackRepository;
    @Autowired ArtistRepository artistRepository;
    @Autowired AppClock clock;
    @Autowired WeightService weightService;
    @Autowired ListeningLogRepository logRepository;

    Long userId;

    @BeforeEach
    void setUp() {
        userId = authService.devLogin("tester").getId();
    }

    private Long genre(String code) {
        return genreRepository.findByCode(code).orElseThrow().getId();
    }

    private List<Long> genres(String... codes) {
        return Arrays.stream(codes).map(this::genre).toList();
    }

    private Long track(String title) {
        return trackRepository.findByTitle(title).orElseThrow().getId();
    }

    private Long artist(String name) {
        return artistRepository.findByName(name).orElseThrow().getId();
    }

    private List<Long> activeGenres(RoadmapResponse r) {
        return r.steps().stream().filter(s -> s.status() != StepStatus.REPLACED).map(StepResponse::genreId).toList();
    }

    private StepResponse currentStep() {
        RoadmapResponse r = roadmapService.current(userId).orElseThrow();
        return r.steps().stream()
                .filter(s -> s.stepIndex() == r.currentStepIndex() && s.status() != StepStatus.REPLACED)
                .findFirst().orElseThrow();
    }

    private List<String> titles(List<Long> trackIds) {
        return trackIds.stream().map(id -> trackRepository.findById(id).orElseThrow().getTitle()).toList();
    }

    private FeedbackResponse listen(String title, Feedback feedback, DislikeReason reason) {
        return roadmapService.submitFeedback(userId, new ListenRequest(track(title), feedback, reason, ""));
    }

    /** 브릿 인베이전 → 브릿팝: 1번 스텝이 브릿팝 */
    private RoadmapResponse startBritpopStep() {
        return roadmapService.create(userId, new CreateRoadmapRequest(
                ExplorationType.BRIDGE, genre("BRITISH_INVASION"), genre("BRITPOP"), 5));
    }

    @Test
    void C_두_장르_잇기_로드맵_생성() {
        RoadmapResponse r = roadmapService.create(userId, new CreateRoadmapRequest(
                ExplorationType.BRIDGE, genre("BRITPOP"), genre("HIP_HOP"), 5));

        assertThat(activeGenres(r)).isEqualTo(genres("BRITPOP", "BRITISH_INVASION", "SOUL", "FUNK", "HIP_HOP"));
        assertThat(r.currentStepIndex()).isEqualTo(1);
        assertThat(titles(currentStep().trackIds()))
                .containsExactly("I Want to Hold Your Hand", "You Really Got Me", "(I Can't Get No) Satisfaction");
    }

    @Test
    void 진행_중인_로드맵이_있으면_새로_만들_수_없다() {
        startBritpopStep();
        assertThatThrownBy(this::startBritpopStep)
                .extracting("errorCode").isEqualTo(ErrorCode.ROADMAP_ALREADY_IN_PROGRESS);
    }

    @Test
    void 가지_바꾸기_싫어하면_직전_장르로_돌아가_다른_가지로() {
        roadmapService.create(userId, new CreateRoadmapRequest(
                ExplorationType.DESCENDANTS, genre("RNB"), null, 4));
        assertThat(activeGenres(roadmapService.current(userId).orElseThrow()))
                .isEqualTo(genres("RNB", "DOO_WOP", "SOUL", "PSYCHEDELIC", "PROG"));

        for (String title : titles(currentStep().trackIds())) {
            listen(title, Feedback.DISLIKE, DislikeReason.TRACK);
        }
        // 기획서 19장: 반응만으로는 넘어가지 않고 "다음 장르로"에서 판정한다 (verdict 생략 → 반응 다수결)
        StepResultResponse result = roadmapService.next(userId, null);

        assertThat(result.verdict()).isEqualTo(Verdict.DISLIKED);
        assertThat(result.roadmapChanged()).isTrue();
        RoadmapResponse r = roadmapService.current(userId).orElseThrow();
        assertThat(r.version()).isEqualTo(2);
        assertThat(activeGenres(r)).isEqualTo(genres("RNB", "DOO_WOP", "ROCKABILLY", "BRITISH_INVASION", "FOLK_ROCK"));
    }

    @Test
    void 선호_16_1_Oasis를_싫어하면_대표곡에서_빠지고_예비곡이_올라온다() {
        preferenceService.save(userId, TargetType.ARTIST, artist("Oasis"), Preference.DISLIKE);
        startBritpopStep();

        StepResponse step = currentStep();
        assertThat(titles(step.trackIds())).containsExactly("Parklife", "Common People", "Animal Nitrate");
        assertThat(step.spareTrackId()).isNull();
    }

    @Test
    void 선호_16_2_가수를_싫어해도_좋아하는_곡은_준다() {
        preferenceService.save(userId, TargetType.ARTIST, artist("Oasis"), Preference.DISLIKE);
        preferenceService.save(userId, TargetType.TRACK, track("Wonderwall"), Preference.LIKE);
        startBritpopStep();

        assertThat(titles(currentStep().trackIds())).contains("Wonderwall");
    }

    @Test
    void 선호_16_3_가수_이유_별로는_장르_판정에서_중립_그리고_가수_싫어함_저장() {
        startBritpopStep();
        FeedbackResponse first = listen("Wonderwall", Feedback.DISLIKE, DislikeReason.ARTIST);
        listen("Parklife", Feedback.DISLIKE, DislikeReason.ARTIST);
        FeedbackResponse last = listen("Common People", Feedback.LIKE, null);

        assertThat(first.dislikedArtistId()).isEqualTo(artist("Oasis"));
        assertThat(last.suggestedVerdict()).isEqualTo(Verdict.NEUTRAL);
        assertThat(preferenceService.list(userId))
                .anyMatch(p -> p.targetType() == TargetType.ARTIST && p.targetId().equals(artist("Oasis"))
                        && p.preference() == Preference.DISLIKE);
    }

    @Test
    void 선호_16_4_곡_이유_별로는_장르_판정에_들어간다() {
        startBritpopStep();
        listen("Wonderwall", Feedback.DISLIKE, DislikeReason.TRACK);
        listen("Parklife", Feedback.DISLIKE, DislikeReason.TRACK);
        FeedbackResponse last = listen("Common People", Feedback.LIKE, null);

        assertThat(last.suggestedVerdict()).isEqualTo(Verdict.DISLIKED);
    }

    @Test
    void 선호_16_5_장르의_모든_가수를_싫어하면_그_스텝은_건너뛴다() {
        for (String name : List.of("Oasis", "Blur", "Pulp", "Suede")) {
            preferenceService.save(userId, TargetType.ARTIST, artist(name), Preference.DISLIKE);
        }
        RoadmapResponse r = startBritpopStep();

        assertThat(roadmapService.current(userId)).isEmpty();
        StepResponse britpop = r.steps().get(1);
        assertThat(britpop.status()).isEqualTo(StepStatus.DONE);
        assertThat(britpop.verdict()).isEqualTo(Verdict.NEUTRAL);
    }

    @Test
    void 선호_16_7_없는_가수에_선호를_저장하면_에러() {
        assertThatThrownBy(() -> preferenceService.save(userId, TargetType.ARTIST, 99_999L, Preference.LIKE))
                .extracting("errorCode").isEqualTo(ErrorCode.TARGET_NOT_FOUND);
    }

    @Test
    void 하루_제한_없이_계속_넘어가고_오늘_넘긴_장르_수를_알려준다() {
        roadmapService.create(userId, new CreateRoadmapRequest(
                ExplorationType.BRIDGE, genre("BRITPOP"), genre("HIP_HOP"), 5));
        roadmapService.next(userId, Verdict.LIKED);
        roadmapService.next(userId, null);
        roadmapService.next(userId, null);

        TodayResponse today = roadmapService.today(userId).orElseThrow();
        assertThat(today.genresCompletedToday()).isEqualTo(3);   // 앱은 3개부터 "쉬어 가도 좋아요" 안내
        assertThat(today.trackIds()).isNotEmpty();                 // 막지 않는다

        clock.skipDay();
        assertThat(roadmapService.today(userId).orElseThrow().genresCompletedToday()).isZero();
    }

    // --- 기획서 19장: 다음 장르로 / 되돌아보기 / 직전 판정 바꾸기 ---------------------------------------

    @Test
    void 다음_장르로는_반응_없이도_넘어가고_중립이면_경로가_그대로() {
        roadmapService.create(userId, new CreateRoadmapRequest(
                ExplorationType.BRIDGE, genre("BRITPOP"), genre("HIP_HOP"), 5));

        StepResultResponse result = roadmapService.next(userId, null);

        assertThat(result.verdict()).isEqualTo(Verdict.NEUTRAL);
        assertThat(result.roadmapChanged()).isFalse();
        assertThat(roadmapService.current(userId).orElseThrow().currentStepIndex()).isEqualTo(2);
    }

    @Test
    void 되돌아보기_지나온_장르의_곡에_반응을_남기고_고칠_수_있고_판정은_그대로() {
        roadmapService.create(userId, new CreateRoadmapRequest(
                ExplorationType.BRIDGE, genre("BRITPOP"), genre("HIP_HOP"), 5));
        roadmapService.next(userId, Verdict.NEUTRAL);   // 브릿 인베이전: 반응 없이 넘어감

        FeedbackResponse added = listen("I Want to Hold Your Hand", Feedback.LIKE, null);
        FeedbackResponse edited = listen("I Want to Hold Your Hand", Feedback.DISLIKE, DislikeReason.TRACK);

        assertThat(added.reviewed()).isTrue();
        assertThat(edited.reviewed()).isTrue();
        RoadmapResponse r = roadmapService.current(userId).orElseThrow();
        assertThat(r.steps().get(1).verdict()).isEqualTo(Verdict.NEUTRAL);   // 판정은 그대로
        assertThat(r.currentStepIndex()).isEqualTo(2);                        // 진행 위치도 그대로
        assertThat(logRepository.findByUserIdOrderByListenedAtDesc(userId))
                .singleElement()
                .satisfies(log -> assertThat(log.getFeedback()).isEqualTo(Feedback.DISLIKE));   // 새로 만들지 않고 고침
    }

    @Test
    void 직전_판정_바꾸기_싫어함을_좋아함으로_바꾸면_경로와_가중치가_복원된_뒤_다시_계산된다() {
        roadmapService.create(userId, new CreateRoadmapRequest(
                ExplorationType.DESCENDANTS, genre("RNB"), null, 4));
        roadmapService.next(userId, Verdict.DISLIKED);
        assertThat(activeGenres(roadmapService.current(userId).orElseThrow()))
                .isEqualTo(genres("RNB", "DOO_WOP", "ROCKABILLY", "BRITISH_INVASION", "FOLK_ROCK"));
        assertThat(roadmapService.today(userId).orElseThrow().canChangeLastVerdict()).isTrue();

        StepResultResponse changed = roadmapService.changeLastVerdict(userId, Verdict.LIKED);

        assertThat(changed.verdict()).isEqualTo(Verdict.LIKED);
        RoadmapResponse r = roadmapService.current(userId).orElseThrow();
        // 좋아함: 두왑의 이웃(R&B, 소울, 펑크) ×0.8 → 두왑에서 이어서: 소울 → 펑크(0.8이 사이키델릭 1.0보다 쌈) → 디스코
        assertThat(activeGenres(r)).isEqualTo(genres("RNB", "DOO_WOP", "SOUL", "FUNK", "DISCO"));
        assertThat(r.version()).isEqualTo(2);
        // 싫어함 가중치(×3)는 스냅샷으로 복원되어 남지 않는다
        assertThat(weightService.load(userId).of(genre("DOO_WOP"))).isEqualTo(1.0);
        assertThat(weightService.load(userId).of(genre("SOUL"))).isEqualTo(0.8);
    }

    @Test
    void 다음_장르에서_반응하면_직전_판정은_바꿀_수_없다() {
        roadmapService.create(userId, new CreateRoadmapRequest(
                ExplorationType.BRIDGE, genre("BRITPOP"), genre("HIP_HOP"), 5));
        roadmapService.next(userId, Verdict.LIKED);
        listen(titles(currentStep().trackIds()).get(0), Feedback.LIKE, null);

        assertThat(roadmapService.today(userId).orElseThrow().canChangeLastVerdict()).isFalse();
        assertThatThrownBy(() -> roadmapService.changeLastVerdict(userId, Verdict.DISLIKED))
                .extracting("errorCode").isEqualTo(ErrorCode.VERDICT_CHANGE_NOT_ALLOWED);
    }
}

