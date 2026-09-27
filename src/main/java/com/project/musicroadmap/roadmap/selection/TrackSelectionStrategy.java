package com.project.musicroadmap.roadmap.selection;

import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.preference.UserPreferences;
import java.util.List;

/** 기획서 16.4: 스텝이 시작될 때 곡을 고른다 */
public interface TrackSelectionStrategy {

    TrackSelectionType type();

    TrackSelection select(List<Track> genreTracks, UserPreferences preferences);
}
