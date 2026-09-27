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

    public GenreGraph(List<Node> nodes, List<Edge> edges) {
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
