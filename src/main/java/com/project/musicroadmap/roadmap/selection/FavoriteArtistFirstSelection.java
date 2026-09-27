package com.project.musicroadmap.roadmap.selection;

import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.preference.UserPreferences;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** 좋아하는 곡·가수를 먼저. 같은 그룹 안에서는 기본 순서 유지 (안정 정렬) */
@Component
public class FavoriteArtistFirstSelection extends OrderedTrackSelection {

    @Override
    public TrackSelectionType type() {
        return TrackSelectionType.FAVORITE_ARTIST_FIRST;
    }

    @Override
    protected List<Track> order(List<Track> candidates, UserPreferences preferences) {
        return candidates.stream()
                .sorted(Comparator.comparingInt(t -> preferences.isFavorite(t) ? 0 : 1))
                .toList();
    }
}
