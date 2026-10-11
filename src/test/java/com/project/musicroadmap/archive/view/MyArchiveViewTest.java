package com.project.musicroadmap.archive.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.musicroadmap.archive.view.MyArchiveService.ArchiveViewResponse;
import com.project.musicroadmap.auth.AuthService;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.personal.PersonalDtos.GenreOverrideRequest;
import com.project.musicroadmap.personal.PersonalDtos.ReviewRequest;
import com.project.musicroadmap.personal.PersonalDtos.TagRequest;
import com.project.musicroadmap.personal.PersonalService;
import com.project.musicroadmap.personal.PersonalTargetType;
import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.PreferenceService;
import com.project.musicroadmap.preference.TargetType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 기획서 23장: 같은 아카이브를 보기 전략마다 다르게 묶는다 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MyArchiveViewTest {

    @Autowired MyArchiveService archiveService;
    @Autowired PersonalService personalService;
    @Autowired PreferenceService preferenceService;
    @Autowired AuthService authService;
    @Autowired ArtistRepository artistRepository;
    @Autowired GenreRepository genreRepository;

    Long me;

    @BeforeEach
    void setUp() {
        me = authService.devLogin("me").getId();
        Long oasis = artist("Oasis");
        Long muddy = artist("Muddy Waters");
        preferenceService.save(me, TargetType.ARTIST, oasis, Preference.LIKE);
        personalService.saveReview(me, new ReviewRequest(PersonalTargetType.ARTIST, muddy, 5, "전기 블루스"));
        personalService.addTag(me, new TagRequest(PersonalTargetType.ARTIST, muddy, "비 오는 날"));
        personalService.addTag(me, new TagRequest(PersonalTargetType.GENRE, genre("BRITPOP"), "비 오는 날"));
        personalService.addTag(me, new TagRequest(PersonalTargetType.ARTIST, oasis, "운동할 때"));
    }

    private Long artist(String name) {
        return artistRepository.findByName(name).orElseThrow().getId();
    }

    private Long genre(String code) {
        return genreRepository.findByCode(code).orElseThrow().getId();
    }

    private static List<String> titles(ArchiveViewResponse r) {
        return r.sections().stream().map(ArchiveSection::title).toList();
    }

    @Test
    void 시대순() {
        ArchiveViewResponse r = archiveService.view(me, ArchiveViewType.TIMELINE);
        assertThat(r.itemCount()).isEqualTo(3);
        assertThat(titles(r)).containsExactly("1950년대", "1990년대");
        assertThat(r.sections().get(1).items()).extracting(ArchiveItem::name).containsExactly("브릿팝", "Oasis");
    }

    @Test
    void 장르별은_내_분류를_따른다() {
        assertThat(titles(archiveService.view(me, ArchiveViewType.BY_GENRE))).containsExactly("블루스", "브릿팝");

        personalService.saveOverride(me, new GenreOverrideRequest(artist("Oasis"), genre("INDIE"), null));
        assertThat(titles(archiveService.view(me, ArchiveViewType.BY_GENRE))).containsExactly("블루스", "인디 록", "브릿팝");
    }

    @Test
    void 태그별은_많이_붙인_태그부터() {
        ArchiveViewResponse r = archiveService.view(me, ArchiveViewType.BY_TAG);
        assertThat(titles(r)).containsExactly("#비 오는 날", "#운동할 때");
        assertThat(r.sections().get(0).items()).extracting(ArchiveItem::name).containsExactly("Muddy Waters", "브릿팝");
    }

    @Test
    void 기록이_없으면_빈_아카이브() {
        Long other = authService.devLogin("other").getId();
        ArchiveViewResponse r = archiveService.view(other, ArchiveViewType.TIMELINE);
        assertThat(r.itemCount()).isZero();
        assertThat(r.sections()).isEmpty();
        assertThat(r.views()).containsExactlyInAnyOrder(ArchiveViewType.values());
    }
}
