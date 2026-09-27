package com.project.musicroadmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.external.musicbrainz.MbArtist;
import com.project.musicroadmap.external.musicbrainz.MbGenre;
import com.project.musicroadmap.external.musicbrainz.MusicBrainzClient;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.DataSource;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.genre.importer.ArtistImportDtos.ExternalArtistResponse;
import com.project.musicroadmap.genre.importer.ArtistImportDtos.ImportArtistResponse;
import com.project.musicroadmap.genre.importer.ArtistImportService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** 실제 MusicBrainz 대신 가짜 클라이언트로 가져오기 흐름을 검증한다 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ArtistImportServiceTest {

    private static final String ARCTIC_MBID = "ada7a83c-e3e1-40f1-93f9-3e73dbc9298a";
    private static final String OASIS_MBID = "39ab1aed-75e0-4140-bd47-540276886b60";
    private static final String KPOP_MBID = "11111111-2222-3333-4444-555555555555";

    @MockitoBean MusicBrainzClient musicBrainzClient;
    @Autowired ArtistImportService importService;
    @Autowired ArtistRepository artistRepository;
    @Autowired GenreRepository genreRepository;

    private Long genre(String code) {
        return genreRepository.findByCode(code).orElseThrow().getId();
    }

    @Test
    void 새_아티스트를_가져오면_계보도_장르에_연결해_저장한다() {
        when(musicBrainzClient.getArtist(ARCTIC_MBID)).thenReturn(new MbArtist(ARCTIC_MBID, "Arctic Monkeys", "Group", "GB", "",
                null, List.of(new MbGenre("indie rock", 19), new MbGenre("alternative rock", 9))));

        ImportArtistResponse first = importService.importArtist(ARCTIC_MBID);

        assertThat(first.primaryGenreId()).isEqualTo(genre("INDIE"));
        assertThat(first.source()).isEqualTo(DataSource.MUSICBRAINZ);
        assertThat(first.externalGenres()).containsExactly("indie rock", "alternative rock");

        // 두 번째부터는 외부 호출 없이 DB 값을 돌려준다
        ImportArtistResponse second = importService.importArtist(ARCTIC_MBID);
        assertThat(second.artistId()).isEqualTo(first.artistId());
        verify(musicBrainzClient, org.mockito.Mockito.times(1)).getArtist(ARCTIC_MBID);
    }

    @Test
    void 큐레이션_가수는_시작할_때_MusicBrainz_ID가_모두_채워진다() {
        assertThat(artistRepository.findAll().stream()
                .filter(a -> a.getSource() == DataSource.CURATED)
                .filter(a -> a.getMbid() == null))
                .isEmpty();
    }

    @Test
    void 큐레이션_가수를_가져오면_외부_호출_없이_기존_가수를_돌려준다() {
        Long curatedOasisId = artistRepository.findByName("Oasis").orElseThrow().getId();

        ImportArtistResponse result = importService.importArtist(OASIS_MBID);

        assertThat(result.artistId()).isEqualTo(curatedOasisId);
        assertThat(result.source()).isEqualTo(DataSource.CURATED);
        assertThat(result.primaryGenreId()).isEqualTo(genre("BRITPOP"));
        verify(musicBrainzClient, never()).getArtist(OASIS_MBID);
    }

    @Test
    void 계보도에_맞는_장르가_없으면_장르_없이_저장한다() {
        when(musicBrainzClient.getArtist(KPOP_MBID)).thenReturn(new MbArtist(KPOP_MBID, "Some K-pop Group", "Group", "KR", "",
                null, List.of(new MbGenre("k-pop", 10))));

        assertThat(importService.importArtist(KPOP_MBID).primaryGenreId()).isNull();
    }

    @Test
    void 검색_결과에는_이미_가져온_아티스트를_표시한다() {
        when(musicBrainzClient.getArtist(ARCTIC_MBID)).thenReturn(new MbArtist(ARCTIC_MBID, "Arctic Monkeys", "Group", "GB", "",
                null, List.of(new MbGenre("indie rock", 19))));
        Long importedId = importService.importArtist(ARCTIC_MBID).artistId();

        when(musicBrainzClient.searchArtists(anyString(), anyInt())).thenReturn(List.of(
                new MbArtist(ARCTIC_MBID, "Arctic Monkeys", "Group", "GB", "", 100, null),
                new MbArtist("22222222-3333-4444-5555-666666666666", "Arctic Monkeys Tribute", "Group", "US", "tribute band", 60, null)));

        List<ExternalArtistResponse> results = importService.searchExternal("arctic monkeys");

        assertThat(results.get(0).importedArtistId()).isEqualTo(importedId);
        assertThat(results.get(0).description()).isEqualTo("Group · GB");
        assertThat(results.get(1).importedArtistId()).isNull();
        assertThat(results.get(1).description()).isEqualTo("Group · US · tribute band");
    }

    @Test
    void 빈_검색어는_외부_호출_없이_거절() {
        assertThatThrownBy(() -> importService.searchExternal("  "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(musicBrainzClient, never()).searchArtists(anyString(), anyInt());
    }
}
