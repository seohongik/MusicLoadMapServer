package com.project.musicroadmap.preference;

import com.project.musicroadmap.genre.Track;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 선호 조회용 값 객체. 기획서 16.2 우선순위: 곡 > 가수 > 장르 */
public class UserPreferences {

    private record Key(TargetType type, long id) {
    }

    private final Map<Key, Preference> byTarget = new HashMap<>();

    public UserPreferences(List<UserPreference> preferences) {
        preferences.forEach(p -> byTarget.put(new Key(p.getTargetType(), p.getTargetId()), p.getPreference()));
    }

    public static UserPreferences empty() {
        return new UserPreferences(List.of());
    }

    public Preference of(TargetType type, long id) {
        return byTarget.get(new Key(type, id));
    }

    public boolean isExcluded(Track track) {
        Preference trackPreference = of(TargetType.TRACK, track.getId());
        if (trackPreference != null) {
            return trackPreference == Preference.DISLIKE;
        }
        return of(TargetType.ARTIST, track.getArtist().getId()) == Preference.DISLIKE;
    }

    public boolean isFavorite(Track track) {
        return of(TargetType.TRACK, track.getId()) == Preference.LIKE
                || of(TargetType.ARTIST, track.getArtist().getId()) == Preference.LIKE;
    }
}
