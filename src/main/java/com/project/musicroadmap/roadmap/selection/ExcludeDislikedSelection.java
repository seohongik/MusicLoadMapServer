package com.project.musicroadmap.roadmap.selection;

import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.preference.UserPreferences;
import java.util.List;
import org.springframework.stereotype.Component;

/** 기본: 싫어하는 곡·가수만 빼고 기본 순서대로 */
@Component
public class ExcludeDislikedSelection extends OrderedTrackSelection {

    @Override
    public TrackSelectionType type() {
        return TrackSelectionType.EXCLUDE_DISLIKED;
    }

    @Override
    protected List<Track> order(List<Track> candidates, UserPreferences preferences) {
        return candidates;
    }
}
