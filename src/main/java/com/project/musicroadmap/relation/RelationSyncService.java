package com.project.musicroadmap.relation;

import com.project.musicroadmap.common.AppClock;
import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.external.musicbrainz.MbArtistRelations;
import com.project.musicroadmap.external.musicbrainz.MusicBrainzClient;
import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.ArtistRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MusicBrainz 관계를 노드·간선으로 저장한다 (참조 레이어, 기획서 24장).
 * 외부 호출은 트랜잭션 밖에서, 저장만 짧은 트랜잭션으로 (14.2와 같은 원칙).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationSyncService {

    static final Duration REFRESH = Duration.ofDays(30);

    private final MusicBrainzClient musicBrainzClient;
    private final ArtistNodeRepository nodeRepository;
    private final ArtistRelationRepository relationRepository;
    private final ArtistRepository artistRepository;
    private final TransactionTemplate transactionTemplate;
    private final AppClock clock;

    /** 우리 DB 가수의 노드 (MusicBrainz ID가 없으면 관계를 가져올 수 없어 빈 값) */
    public Optional<ArtistNode> nodeOf(Artist artist) {
        if (artist.getMbid() == null) {
            return Optional.empty();
        }
        return Optional.of(transactionTemplate.execute(status -> {
            ArtistNode node = nodeRepository.findByMbid(artist.getMbid())
                    .orElseGet(() -> nodeRepository.save(ArtistNode.of(artist.getMbid(), artist.getName(), null, artist.getId())));
            node.linkArtist(artist.getId());
            return node;
        }));
    }

    public boolean needsSync(ArtistNode node) {
        LocalDateTime synced = node.getRelationsSyncedAt();
        return synced == null || synced.isBefore(clock.now().minus(REFRESH));
    }

    /** 노드의 관계를 가져와 저장한다. 외부 서비스가 안 되면 기록하지 않고 false */
    public boolean sync(ArtistNode node) {
        MbArtistRelations response;
        try {
            response = musicBrainzClient.getArtistRelations(node.getMbid());
        } catch (BusinessException e) {
            log.warn("관계를 가져오지 못했어요: {} ({})", node.getName(), e.getErrorCode());
            return false;
        }
        transactionTemplate.executeWithoutResult(status -> save(node.getId(), response));
        return true;
    }

    private void save(Long nodeId, MbArtistRelations response) {
        ArtistNode self = nodeRepository.findById(nodeId).orElseThrow();
        self.assignTypeIfMissing(response.type());
        int added = 0;
        for (MbArtistRelations.Relation r : response.relationsOrEmpty()) {
            if (r.artist() == null || r.artist().id() == null) {
                continue;
            }
            Optional<RelationType> type = RelationType.fromMusicBrainz(r.type());
            if (type.isEmpty()) {
                continue;
            }
            ArtistNode other = nodeRepository.findByMbid(r.artist().id())
                    .orElseGet(() -> nodeRepository.save(ArtistNode.of(r.artist().id(), r.artist().name(), r.artist().type(),
                            artistRepository.findByMbid(r.artist().id()).map(Artist::getId).orElse(null))));
            // forward: 나 → 상대 (내가 그 밴드의 멤버), backward: 상대 → 나
            boolean forward = "forward".equals(r.direction());
            ArtistRelation relation = forward
                    ? ArtistRelation.of(self.getId(), other.getId(), type.get())
                    : ArtistRelation.of(other.getId(), self.getId(), type.get());
            if (!relationRepository.existsByFromNodeIdAndToNodeIdAndType(relation.getFromNodeId(), relation.getToNodeId(), relation.getType())) {
                relationRepository.save(relation);
                added++;
            }
        }
        self.markSynced(clock.now());
        log.info("관계 동기화: {} (+{})", self.getName(), added);
    }
}
