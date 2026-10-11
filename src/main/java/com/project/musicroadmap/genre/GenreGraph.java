package com.project.musicroadmap.genre;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 메모리에 올린 장르 그래프 (기획서 2.1). 만든 뒤에는 바뀌지 않아 여러 요청이 동시에 써도 안전하다.
 * 유저별 가중치는 여기에 넣지 않고 {@link com.project.musicroadmap.weight.UserWeights}로 따로 넘긴다 (15.4).
 */
public class GenreGraph {

    public record Node(long id, String nameKo, int originDecade) {
    }

    public record Edge(long fromId, long toId, double baseCost) {
    }

    private final Map<Long, Node> nodes = new HashMap<>();
    private final Map<Long, List<Long>> parents = new HashMap<>();
    private final Map<Long, List<Long>> children = new HashMap<>();
    private final Map<String, Double> costs = new HashMap<>();
    private final List<Edge> edges;

    public GenreGraph(List<Node> nodes, List<Edge> edges) {
        this.edges = List.copyOf(edges);
        nodes.forEach(node -> this.nodes.put(node.id(), node));
        for (Edge edge : edges) {
            children.computeIfAbsent(edge.fromId(), k -> new ArrayList<>()).add(edge.toId());
            parents.computeIfAbsent(edge.toId(), k -> new ArrayList<>()).add(edge.fromId());
            costs.put(key(edge.fromId(), edge.toId()), edge.baseCost());
        }
    }

    public boolean contains(long id) {
        return nodes.containsKey(id);
    }

    public Node node(long id) {
        Node node = nodes.get(id);
        if (node == null) {
            throw new BusinessException(ErrorCode.GENRE_NOT_FOUND);
        }
        return node;
    }

    public List<Long> parents(long id) {
        return parents.getOrDefault(id, List.of());
    }

    public List<Long> children(long id) {
        return children.getOrDefault(id, List.of());
    }

    public List<Long> neighbors(long id) {
        LinkedHashSet<Long> result = new LinkedHashSet<>(parents(id));
        result.addAll(children(id));
        return new ArrayList<>(result);
    }

    /** 간선 방향과 상관없이 두 장르가 이어져 있는지 */
    public boolean connected(long a, long b) {
        return costs.containsKey(key(a, b)) || costs.containsKey(key(b, a));
    }

    /** from → to 간선을 넣으면 순환이 생기는지 (to에서 자식을 따라 from에 닿으면 순환) */
    public boolean wouldCreateCycle(long from, long to) {
        if (from == to) {
            return true;
        }
        java.util.ArrayDeque<Long> queue = new java.util.ArrayDeque<>(List.of(to));
        java.util.Set<Long> seen = new java.util.HashSet<>(List.of(to));
        while (!queue.isEmpty()) {
            for (Long child : children(queue.poll())) {
                if (child == from) {
                    return true;
                }
                if (seen.add(child)) {
                    queue.add(child);
                }
            }
        }
        return false;
    }

    /**
     * 기획서 15.4: 이 그래프는 그대로 두고 간선을 더한 새 그래프를 만든다 (유저별로 덧씌운 뷰).
     * 공통 그래프는 여러 유저가 동시에 쓰므로 절대 고치지 않는다.
     */
    public GenreGraph withExtraEdges(List<Edge> extra) {
        if (extra.isEmpty()) {
            return this;
        }
        List<Edge> all = new ArrayList<>(edges);
        extra.stream().filter(e -> !connected(e.fromId(), e.toId())).forEach(all::add);
        return new GenreGraph(new ArrayList<>(nodes.values()), all);
    }

    /** 간선 방향과 상관없이 두 장르 사이의 기본 비용 */
    public double baseCost(long a, long b) {
        Double cost = costs.getOrDefault(key(a, b), costs.get(key(b, a)));
        if (cost == null) {
            throw new IllegalStateException("No edge between " + a + " and " + b);
        }
        return cost;
    }

    private static String key(long from, long to) {
        return from + ":" + to;
    }
}
