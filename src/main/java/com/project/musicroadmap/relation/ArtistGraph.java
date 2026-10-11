package com.project.musicroadmap.relation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 요청마다 DB의 관계로 만드는 메모리 그래프 (방향 무시). 노드 수가 커지면 깊이 제한·부분 조회로 바꾼다 (14.4) */
public class ArtistGraph {

    /** forward: 저장된 방향 그대로 (MEMBER면 "나는 to의 멤버") */
    public record Link(long to, RelationType type, boolean forward) {
    }

    private final Map<Long, ArtistNode> nodes = new HashMap<>();
    private final Map<Long, List<Link>> links = new HashMap<>();

    public ArtistGraph(List<ArtistNode> nodes, List<ArtistRelation> relations) {
        nodes.forEach(n -> this.nodes.put(n.getId(), n));
        for (ArtistRelation r : relations) {
            links.computeIfAbsent(r.getFromNodeId(), k -> new ArrayList<>()).add(new Link(r.getToNodeId(), r.getType(), true));
            links.computeIfAbsent(r.getToNodeId(), k -> new ArrayList<>()).add(new Link(r.getFromNodeId(), r.getType(), false));
        }
    }

    public ArtistNode node(long id) {
        return nodes.get(id);
    }

    public List<Link> links(long id) {
        return links.getOrDefault(id, List.of());
    }

    /** a에서 b로 가는 링크 (여럿이면 첫 번째) */
    public Link linkBetween(long a, long b) {
        return links(a).stream().filter(l -> l.to() == b).findFirst().orElse(null);
    }

    /** a에서 b로 갈 때의 설명: 소속 밴드 / 멤버 / 협업 */
    public static String label(Link link) {
        if (link == null) {
            return "";
        }
        if (link.type() == RelationType.COLLABORATION) {
            return RelationType.COLLABORATION.label();
        }
        return link.forward() ? "소속 밴드" : "멤버";
    }
}
