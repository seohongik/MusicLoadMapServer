package com.project.musicroadmap.personal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.musicroadmap.archive.ArchiveDtos.ArtistDetailResponse;
import com.project.musicroadmap.archive.ArchiveService;
import com.project.musicroadmap.auth.AuthService;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.external.musicbrainz.MusicBrainzClient;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.personal.PersonalDtos.GenreOverrideRequest;
import com.project.musicroadmap.personal.PersonalDtos.ReviewRequest;
import com.project.musicroadmap.personal.PersonalDtos.TagRequest;
import com.project.musicroadmap.personal.PersonalDtos.TagSummary;
import com.project.musicroadmap.personal.PersonalDtos.TaggedTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** 기획서 15장, 22장: 개인 레이어는 참조 레이어를 고치지 않고 겹쳐서 보여준다 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersonalServiceTest {

    @Autowired PersonalService personalService;
    @Autowired ArchiveService archiveService;
    @Autowired AuthService authService;
    @Autowired ArtistRepository artistRepository;
    @Autowired GenreRepository genreRepository;
    @MockitoBean MusicBrainzClient musicBrainzClient;

    Long me;
    Long other;
    Long oasis;

    @BeforeEach
    void setUp() {
        me = authService.devLogin("me").getId();
        other = authService.devLogin("other").getId();
        oasis = artistRepository.findByName("Oasis").orElseThrow().getId();
    }

    private Long genre(String code) {
        return genreRepository.findByCode(code).orElseThrow().getId();
    }

    @Test
    void 감상은_대상마다_하나이고_다시_쓰면_고쳐진다() {
        personalService.saveReview(me, new ReviewRequest(PersonalTargetType.ARTIST, oasis, 4, "  형제 싸움도 음악  "));
        personalService.saveReview(me, new ReviewRequest(PersonalTargetType.ARTIST, oasis, 5, "다시 들으니 최고"));

        assertThat(personalService.reviews(me)).singleElement().satisfies(r -> {
            assertThat(r.rating()).isEqualTo(5);
            assertThat(r.content()).isEqualTo("다시 들으니 최고");
            assertThat(r.targetName()).isEqualTo("Oasis");
        });
        assertThat(personalService.reviews(other)).isEmpty();
    }

    @Test
    void 태그는_정리해서_저장하고_같은_태그는_한_번만_붙는다() {
        personalService.addTag(me, new TagRequest(PersonalTargetType.ARTIST, oasis, "#비 오는   날"));
        personalService.addTag(me, new TagRequest(PersonalTargetType.ARTIST, oasis, "비 오는 날"));
        personalService.addTag(me, new TagRequest(PersonalTargetType.GENRE, genre("BRITPOP"), "비 오는 날"));
        personalService.addTag(me, new TagRequest(PersonalTargetType.GENRE, genre("BRITPOP"), "운동할 때"));

        assertThat(personalService.tags(me)).containsExactly(new TagSummary("비 오는 날", 2), new TagSummary("운동할 때", 1));
        assertThat(personalService.tagged(me, "비 오는 날")).extracting(TaggedTarget::targetName)
                .containsExactlyInAnyOrder("Oasis", "브릿팝");
        assertThat(personalService.removeTag(me, PersonalTargetType.GENRE, genre("BRITPOP"), "#운동할 때"))
                .containsExactly("비 오는 날");
        assertThat(personalService.tags(other)).isEmpty();
    }

    @Test
    void 나만의_분류는_공식_분류를_바꾸지_않고_겹쳐서_보여준다() {
        Long official = artistRepository.findById(oasis).orElseThrow().getPrimaryGenre().getId();
        personalService.saveOverride(me, new GenreOverrideRequest(oasis, genre("INDIE"), "내겐 인디 록"));

        ArtistDetailResponse mine = archiveService.getArtistDetail(me, oasis);
        assertThat(mine.officialGenreId()).isEqualTo(official);
        assertThat(mine.myGenre().genreId()).isEqualTo(genre("INDIE"));
        assertThat(mine.myGenre().note()).isEqualTo("내겐 인디 록");

        ArtistDetailResponse theirs = archiveService.getArtistDetail(other, oasis);
        assertThat(theirs.myGenre()).isNull();
        assertThat(artistRepository.findById(oasis).orElseThrow().getPrimaryGenre().getId()).isEqualTo(official);
    }

    @Test
    void 가수_상세에_내_감상과_태그가_붙는다() {
        personalService.saveReview(me, new ReviewRequest(PersonalTargetType.ARTIST, oasis, null, "처음 산 CD"));
        personalService.addTag(me, new TagRequest(PersonalTargetType.ARTIST, oasis, "추억"));

        ArtistDetailResponse detail = archiveService.getArtistDetail(me, oasis);
        assertThat(detail.myNotes().review().content()).isEqualTo("처음 산 CD");
        assertThat(detail.myNotes().tags()).containsExactly("추억");
    }

    @Test
    void 없는_대상에는_남길_수_없다() {
        assertThatThrownBy(() -> personalService.saveReview(me, new ReviewRequest(PersonalTargetType.ALBUM, 999_999L, null, "x")))
                .extracting("errorCode").isEqualTo(ErrorCode.TARGET_NOT_FOUND);
        assertThatThrownBy(() -> personalService.addTag(me, new TagRequest(PersonalTargetType.ARTIST, oasis, " # ")))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
