package com.project.musicroadmap.weight;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.HashMap;
import java.util.Map;

/** 유저별 장르 가중치 (기획서 2.2). 요청마다 DB에서 읽어 만들고, 바뀌면 WeightService로 저장한다. */
public class UserWeights {

    public static final double DEFAULT = 1.0;
    public static final double DISLIKE_THRESHOLD = 3.0;
    public static final double DISLIKE_FACTOR = 3.0;
    public static final double DESCENDANT_DISLIKE_FACTOR = 1.5;
    public static final double LIKE_FACTOR = 0.8;
    public static final double MIN = 0.5;
    public static final double MAX = 10.0;

    private final Map<Long, Double> values;

    public UserWeights() {
        this(Map.of());
    }

    public UserWeights(Map<Long, Double> initial) {
        this.values = new HashMap<>(initial);
    }

    public double of(long genreId) {
        return values.getOrDefault(genreId, DEFAULT);
    }

    public boolean isDisliked(long genreId) {
        return of(genreId) >= DISLIKE_THRESHOLD;
    }

    /** 싫어한 장르는 ×3, 그 장르에서 파생된 장르(자식)도 ×1.5 */
    public void applyDislike(long genreId, GenreGraph graph) {
        multiply(genreId, DISLIKE_FACTOR);
        graph.children(genreId).forEach(child -> multiply(child, DESCENDANT_DISLIKE_FACTOR));
    }

    /** 좋아한 장르의 이웃(기원·파생 모두) ×0.8 */
    public void applyLike(long genreId, GenreGraph graph) {
        graph.neighbors(genreId).forEach(neighbor -> multiply(neighbor, LIKE_FACTOR));
    }

    public Map<Long, Double> snapshot() {
        return Map.copyOf(values);
    }

    private void multiply(long genreId, double factor) {
        values.put(genreId, Math.max(MIN, Math.min(MAX, of(genreId) * factor)));
    }
}
