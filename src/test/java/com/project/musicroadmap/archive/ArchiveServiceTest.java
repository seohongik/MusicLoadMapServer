package com.project.musicroadmap.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.musicroadmap.archive.ArchiveDtos.AlbumDetailResponse;
import com.project.musicroadmap.archive.ArchiveDtos.ArtistDetailResponse;
import com.project.musicroadmap.auth.AuthService;
import com.project.musicroadmap.external.musicbrainz.MbRelease;
import com.project.musicroadmap.external.musicbrainz.MbRelease.MbMedium;
import com.project.musicroadmap.external.musicbrainz.MbRelease.MbRecording;
import com.project.musicroadmap.external.musicbrainz.MbRelease.MbTrack;
import com.project.musicroadmap.external.musicbrainz.MbReleaseGroup;
import com.project.musicroadmap.external.musicbrainz.MusicBrainzClient;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.PreferenceService;
import com.project.musicroadmap.preference.TargetType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** 기획서 18장. 실제 MusicBrainz 대신 가짜 응답(Oasis 앨범 일부)으로 검증한다 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ArchiveServiceTest {

    private static final String OASIS_MBID = "39ab1aed-75e0-4140-bd47-540276886b60";
    private static final String MORNING_GLORY = "5f4f3f9e-0000-0000-0000-000000000001";

    @MockitoBean MusicBrainzClient musicBrainzClient;
    @Autowired ArchiveService archiveService;
    @Autowired PreferenceService preferenceService;
    @Autowired AuthService authService;
    @Autowired ArtistRepository artistRepository;

    Long userId;
    Long oasisId;

    @BeforeEach
    void setUp() {
        userId = authService.devLogin("archive-tester").getId();
        oasisId = artistRepository.findByName("Oasis").orElseThrow().getId();
        when(musicBrainzClient.browseAlbumGroups(eq(OASIS_MBID), anyInt())).thenReturn(List.of(
                new MbReleaseGroup(MORNING_GLORY, "(What's the Story) Morning Glory?", "Album", List.of(), "1995-10-02"),
                new MbReleaseGroup("5f4f3f9e-0000-0000-0000-000000000002", "Definitely Maybe", "Album", List.of(), "1994-08-29"),
                new MbReleaseGroup("5f4f3f9e-0000-0000-0000-000000000003", "Familiar to Millions", "Album", List.of("Live"), "2000-11-13"),
                new MbReleaseGroup("5f4f3f9e-0000-0000-0000-000000000004", "Stop the Clocks", "Album", List.of("Compilation"), "2006-11-20")));
    }

    @Test
    void 가수_상세는_정규_앨범만_연도순으로_보여주고_두번째부터는_외부_호출이_없다() {
        ArtistDetailResponse first = archiveService.getArtistDetail(userId, oasisId);

        assertThat(first.albumsAvailable()).isTrue();
        assertThat(first.albums()).extracting("title")
                .containsExactly("Definitely Maybe", "(What's the Story) Morning Glory?");
        assertThat(first.albums().get(0).coverUrl()).startsWith("https://coverartarchive.org/release-group/");
        assertThat(first.genreRoles()).hasSize(1);
        assertThat(first.genreRoles().get(0).intro()).contains("브릿팝");

        archiveService.getArtistDetail(userId, oasisId);
        verify(musicBrainzClient, times(1)).browseAlbumGroups(eq(OASIS_MBID), anyInt());
    }

    @Test
    void 앨범_상세는_정확한_날짜의_원래_발매판에서_수록곡을_가져오고_곡별_좋아요_별로를_보여준다() {
        Long albumId = archiveService.getArtistDetail(userId, oasisId).albums().get(1).id();
        when(musicBrainzClient.browseReleases(MORNING_GLORY, true)).thenReturn(List.of(
                new MbRelease("rel-year-only", "Morning Glory", "1995", "XE", null),
                new MbRelease("rel-original", "Morning Glory", "1995-10-02", "GB", null),
                new MbRelease("rel-reissue", "Morning Glory", "2014-09-26", "GB", null)));
        when(musicBrainzClient.getReleaseWithRecordings("rel-original")).thenReturn(new MbRelease("rel-original",
                "Morning Glory", "1995-10-02", "GB", List.of(new MbMedium(1, "CD", List.of(
                        new MbTrack("1", "Hello", 201000, new MbRecording("r1")),
                        new MbTrack("2", "Roll With It", 239000, new MbRecording("r2")),
                        new MbTrack("3", "Wonderwall", 258000, new MbRecording("r3")))))));

        AlbumDetailResponse album = archiveService.getAlbumDetail(userId, albumId);
        assertThat(album.tracks()).extracting("title").containsExactly("Hello", "Roll With It", "Wonderwall");
        assertThat(album.artistName()).isEqualTo("Oasis");

        Long wonderwall = album.tracks().get(2).id();
        Long rollWithIt = album.tracks().get(1).id();
        preferenceService.save(userId, TargetType.ALBUM_TRACK, wonderwall, Preference.LIKE);
        preferenceService.save(userId, TargetType.ALBUM_TRACK, rollWithIt, Preference.DISLIKE);

        AlbumDetailResponse again = archiveService.getAlbumDetail(userId, albumId);
        assertThat(again.tracks()).extracting("myPreference").containsExactly(null, Preference.DISLIKE, Preference.LIKE);
        verify(musicBrainzClient, times(1)).getReleaseWithRecordings(anyString());
        verify(musicBrainzClient, never()).getReleaseWithRecordings("rel-year-only");

        assertThat(preferenceService.list(userId))
                .anyMatch(p -> p.targetType() == TargetType.ALBUM_TRACK
                        && "Wonderwall · (What's the Story) Morning Glory?".equals(p.targetName()));
    }

    @Test
    void 날짜_비교는_연도만_있는_발매판을_그해의_마지막_날로_본다() {
        assertThat(ArchiveService.sortableDate("2013")).isEqualTo("2013-12-31");
        assertThat(ArchiveService.sortableDate("2013-09")).isEqualTo("2013-09-31");
        assertThat(ArchiveService.sortableDate(null)).isEqualTo("9999-12-31");
        assertThat(ArchiveService.chooseRelease(List.of(
                new MbRelease("a", "", "2013", "BR", null),
                new MbRelease("b", "", "2013-09-09", "GB", null))).orElseThrow().id()).isEqualTo("b");
    }
}
