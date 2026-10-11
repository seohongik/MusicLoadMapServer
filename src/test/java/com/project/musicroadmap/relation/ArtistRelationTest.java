package com.project.musicroadmap.relation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.project.musicroadmap.auth.AuthService;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.external.musicbrainz.MbArtistRelations;
import com.project.musicroadmap.external.musicbrainz.MbArtistRelations.RelatedArtist;
import com.project.musicroadmap.external.musicbrainz.MbArtistRelations.Relation;
import com.project.musicroadmap.external.musicbrainz.MusicBrainzClient;
import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.PreferenceService;
import com.project.musicroadmap.preference.TargetType;
import com.project.musicroadmap.relation.ArtistRelationService.ConnectionsResponse;
import com.project.musicroadmap.relation.ArtistRelationService.NodeResponse;
import com.project.musicroadmap.relation.ArtistRelationService.PathResponse;
import com.project.musicroadmap.relation.ArtistRelationService.RelatedResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기획서 24장: 가수 관계 그래프.
 * Oasis ← Noel(멤버) —협업— Damon → Blur(멤버). 사람을 거쳐 밴드끼리 이어진다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ArtistRelationTest {

    @Autowired ArtistRelationService relationService;
    @Autowired RelationBatch relationBatch;
    @Autowired AuthService authService;
    @Autowired ArtistRepository artistRepository;
    @Autowired PreferenceService preferenceService;
    @MockitoBean MusicBrainzClient musicBrainzClient;

    Long me;
    Artist oasis;
    Artist blur;

    private static Relation rel(String type, String direction, String mbid, String name, String kind) {
        return new Relation(type, direction, new RelatedArtist(mbid, name, kind, null));
    }

    @BeforeEach
    void setUp() {
        me = authService.devLogin("me").getId();
        oasis = artistRepository.findByName("Oasis").orElseThrow();
        blur = artistRepository.findByName("Blur").orElseThrow();
        when(musicBrainzClient.getArtistRelations(anyString()))
                .thenAnswer(inv -> new MbArtistRelations(inv.getArgument(0), "?", "Person", List.of()));
        when(musicBrainzClient.getArtistRelations(oasis.getMbid())).thenReturn(new MbArtistRelations(oasis.getMbid(), "Oasis", "Group", List.of(
                rel("member of band", "backward", "p-noel", "Noel Gallagher", "Person"),
                rel("member of band", "backward", "p-liam", "Liam Gallagher", "Person"),
                rel("tribute", "backward", "t-band", "Oasis tribute band", "Group"))));
        when(musicBrainzClient.getArtistRelations(blur.getMbid())).thenReturn(new MbArtistRelations(blur.getMbid(), "Blur", "Group", List.of(
                rel("member of band", "backward", "p-damon", "Damon Albarn", "Person"))));
        when(musicBrainzClient.getArtistRelations("p-noel")).thenReturn(new MbArtistRelations("p-noel", "Noel Gallagher", "Person", List.of(
                rel("member of band", "forward", oasis.getMbid(), "Oasis", "Group"),
                rel("collaboration", "forward", "p-damon", "Damon Albarn", "Person"))));
    }

    @Test
    void 가수_상세를_열면_멤버_관계를_가져오고_트리뷰트는_뺀다() {
        ConnectionsResponse r = relationService.connections(me, oasis.getId());

        assertThat(r.available()).isTrue();
        assertThat(r.related()).extracting(x -> x.node().name()).containsExactly("Liam Gallagher", "Noel Gallagher");
        assertThat(r.related()).extracting(RelatedResponse::label).containsOnly("멤버");
    }

    @Test
    void 관계망이_쌓이면_사람을_거쳐_밴드끼리_이어진다() {
        relationService.connections(me, oasis.getId());
        assertThatThrownBy(() -> relationService.bridge(me, oasis.getId(), blur.getId()))
                .extracting("errorCode").isEqualTo(ErrorCode.NO_ARTIST_PATH);

        relationBatch.run();

        PathResponse path = relationService.bridge(me, oasis.getId(), blur.getId());
        assertThat(path.path()).extracting(NodeResponse::name)
                .containsExactly("Oasis", "Noel Gallagher", "Damon Albarn", "Blur");
        assertThat(path.relations()).containsExactly("멤버", "협업", "소속 밴드");
    }

    @Test
    void 주변_가수는_싫어하는_가수를_빼고_보여준다() {
        relationBatch.run();
        assertThat(relationService.connections(me, oasis.getId()).nearby()).extracting(NodeResponse::name).contains("Blur");

        preferenceService.save(me, TargetType.ARTIST, blur.getId(), Preference.DISLIKE);
        assertThat(relationService.connections(me, oasis.getId()).nearby()).extracting(NodeResponse::name).doesNotContain("Blur");
    }
}
