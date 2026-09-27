package com.project.musicroadmap.roadmap.selection;

import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.preference.UserPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 장르를 키로 묶인 가수(곡) 배열에서 무작위로 고른다.
 * - 싫어하는 곡·가수는 빼고 (OrderedTrackSelection 공통 규칙)
 * - 좋아하는 가수는 빠지지 않도록 앞에 두고, 그 안에서도 섞는다
 * - 스텝이 시작될 때 한 번 뽑아 저장하므로 앱을 다시 열어도 순서가 바뀌지 않는다
 */
@Component
public class RandomSelection extends OrderedTrackSelection {

    private final Random random;

    @Autowired
    public RandomSelection() {
        this(new Random());
    }

    /** 테스트에서 씨앗을 고정해 결과를 재현할 때 */
    public RandomSelection(Random random) {
        this.random = random;
    }

    @Override
    public TrackSelectionType type() {
        return TrackSelectionType.RANDOM;
    }

    @Override
    protected List<Track> order(List<Track> candidates, UserPreferences preferences) {
        List<Track> favorites = new ArrayList<>();
        List<Track> others = new ArrayList<>();
        candidates.forEach(t -> (preferences.isFavorite(t) ? favorites : others).add(t));
        Collections.shuffle(favorites, random);
        Collections.shuffle(others, random);
        favorites.addAll(others);
        return favorites;
    }
}
