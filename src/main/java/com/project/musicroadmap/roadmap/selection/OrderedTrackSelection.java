package com.project.musicroadmap.roadmap.selection;

import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.preference.UserPreferences;
import java.util.Comparator;
import java.util.List;

/** 싫어하는 곡·가수를 빼고, 정렬한 뒤 앞의 3곡을 대표곡, 4번째를 예비곡으로 */
public abstract class OrderedTrackSelection implements TrackSelectionStrategy {

    private static final int MAIN_COUNT = 3;

    protected abstract List<Track> order(List<Track> candidates, UserPreferences preferences);

    @Override
    public TrackSelection select(List<Track> genreTracks, UserPreferences preferences) {
        List<Track> candidates = genreTracks.stream()
                .sorted(Comparator.comparingInt(Track::getDisplayOrder))
                .filter(t -> !preferences.isExcluded(t))
                .toList();
        List<Track> ordered = order(candidates, preferences);
        List<Long> main = ordered.stream().limit(MAIN_COUNT).map(Track::getId).toList();
        Long spare = ordered.size() > MAIN_COUNT ? ordered.get(MAIN_COUNT).getId() : null;
        return new TrackSelection(main, spare);
    }
}
