package com.project.musicroadmap.relation;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.preference.PreferenceService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 기획서 24장: 가수 관계 보기·주변 가수·두 가수 잇기 */
@Service
@RequiredArgsConstructor
public class ArtistRelationService {

    static final int NEARBY_LIMIT = 15;

    private final ArtistRepository artistRepository;
    private final ArtistNodeRepository nodeRepository;
    private final ArtistRelationRepository relationRepository;
    private final RelationSyncService syncService;
    private final ArtistExplorationResolver explorationResolver;
    private final PreferenceService preferenceService;
    private final TransactionTemplate transactionTemplate;

    /** 노드 정보. artistId가 있으면 우리 DB의 가수, 없으면 mbid로 가져올 수 있다 */
    public record NodeResponse(Long nodeId, String name, String type, Long artistId, String mbid) {
        static NodeResponse from(ArtistNode n) {
            return new NodeResponse(n.getId(), n.getName(), n.getType(), n.getArtistId(), n.getMbid());
        }
    }

    /** 바로 이어진 가수와 관계 (예: "멤버" Liam Gallagher) */
    public record RelatedResponse(NodeResponse node, RelationType relation, String label) {
    }

    /** path: 출발~목표 노드, relations: 각 칸 사이의 관계 (path보다 하나 적다) */
    public record PathResponse(List<NodeResponse> path, List<String> relations) {
    }

    public record ConnectionsResponse(boolean available, List<RelatedResponse> related, List<NodeResponse> nearby) {
    }

    /** 가수 상세용: 처음 열 때 관계를 가져온다 */
    public ConnectionsResponse connections(Long userId, Long artistId) {
        Artist artist = artistRepository.findById(artistId).orElseThrow(() -> new BusinessException(ErrorCode.TARGET_NOT_FOUND));
        ArtistNode node = syncService.nodeOf(artist).orElse(null);
        if (node == null) {
            return new ConnectionsResponse(false, List.of(), List.of());
        }
        if (syncService.needsSync(node)) {
            syncService.sync(node);
        }
        return transactionTemplate.execute(status -> {
            ArtistGraph graph = loadGraph();
            ArtistWeights weights = new ArtistWeights(preferenceService.load(userId));
            List<RelatedResponse> related = new ArrayList<>(graph.links(node.getId()).stream()
                    .map(link -> new RelatedResponse(NodeResponse.from(graph.node(link.to())), link.type(),
                            ArtistGraph.label(link)))
                    .distinct()
                    .sorted(Comparator.comparing((RelatedResponse r) -> r.node().artistId() == null)
                            .thenComparing(r -> r.node().name()))
                    .toList());
            List<NodeResponse> nearby = explorationResolver.get(ArtistExplorationType.NEARBY)
                    .explore(graph, weights, node.getId(), null, NEARBY_LIMIT).stream()
                    .map(graph::node)
                    .map(NodeResponse::from)
                    .toList();
            return new ConnectionsResponse(true, related, nearby);
        });
    }

    /** 두 가수 잇기 */
    public PathResponse bridge(Long userId, Long fromArtistId, Long toArtistId) {
        ArtistNode from = requireNode(fromArtistId);
        ArtistNode to = requireNode(toArtistId);
        for (ArtistNode node : List.of(from, to)) {
            if (syncService.needsSync(node)) {
                syncService.sync(node);
            }
        }
        return transactionTemplate.execute(status -> {
            ArtistGraph graph = loadGraph();
            List<Long> path = explorationResolver.get(ArtistExplorationType.BRIDGE)
                    .explore(graph, new ArtistWeights(preferenceService.load(userId)), from.getId(), to.getId(), 0);
            if (path.isEmpty()) {
                throw new BusinessException(ErrorCode.NO_ARTIST_PATH);
            }
            List<String> relations = new ArrayList<>();
            for (int i = 0; i + 1 < path.size(); i++) {
                relations.add(ArtistGraph.label(graph.linkBetween(path.get(i), path.get(i + 1))));
            }
            return new PathResponse(path.stream().map(graph::node).map(NodeResponse::from).toList(), relations);
        });
    }

    private ArtistNode requireNode(Long artistId) {
        Artist artist = artistRepository.findById(artistId).orElseThrow(() -> new BusinessException(ErrorCode.TARGET_NOT_FOUND));
        return syncService.nodeOf(artist).orElseThrow(() -> new BusinessException(ErrorCode.ARTIST_NOT_LINKED));
    }

    private ArtistGraph loadGraph() {
        return new ArtistGraph(nodeRepository.findAll(), relationRepository.findAll());
    }
}
