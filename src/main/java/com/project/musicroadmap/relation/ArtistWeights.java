package com.project.musicroadmap.relation;

import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.TargetType;
import com.project.musicroadmap.preference.UserPreferences;

/** 가수 관계 탐험의 개인 비용 (기획서 24장): 좋아하는 가수는 가깝게, 싫어하는 가수는 피한다 */
public class ArtistWeights {

    static final double LIKED = 0.8;
    static final double DISLIKED = 3.0;

    private final UserPreferences preferences;

    public ArtistWeights(UserPreferences preferences) {
        this.preferences = preferences;
    }

    public double cost(ArtistNode node) {
        if (node == null || node.getArtistId() == null) {
            return 1.0;
        }
        Preference p = preferences.of(TargetType.ARTIST, node.getArtistId());
        if (p == Preference.LIKE) {
            return LIKED;
        }
        return p == Preference.DISLIKE ? DISLIKED : 1.0;
    }

    public boolean isDisliked(ArtistNode node) {
        return cost(node) >= DISLIKED;
    }
}
