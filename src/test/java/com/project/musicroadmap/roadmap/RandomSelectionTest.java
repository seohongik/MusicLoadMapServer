package com.project.musicroadmap.roadmap;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.Genre;
import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.genre.TrackRole;
import com.project.musicroadmap.preference.UserPreferences;
import com.project.musicroadmap.roadmap.selection.RandomSelection;
import com.project.musicroadmap.roadmap.selection.TrackSelection;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 장르(키) → 가수 배열에서 무작위로 뽑기 */
class RandomSelectionTest {

    private final Genre britpop = Genre.curated("BRITPOP", "Britpop", "브릿팝", 1990, "", 0, 0);
    private final List<Track> tracks = List.of(
            track(1, "Oasis", "Wonderwall", 0),
            track(2, "Blur", "Parklife", 1),
            track(3, "Pulp", "Common People", 2),
            track(4, "Suede", "Animal Nitrate", 3));

    @Test
    void 같은_씨앗이면_같은_결과() {
        TrackSelection a = new RandomSelection(new Random(42)).select(tracks, UserPreferences.empty());
        TrackSelection b = new RandomSelection(new Random(42)).select(tracks, UserPreferences.empty());
        assertThat(a).isEqualTo(b);
        assertThat(a.mainTrackIds()).hasSize(3);
        assertThat(a.spareTrackId()).isNotNull();
    }

    @Test
    void 여러_번_뽑으면_모든_가수가_골고루_대표곡에_나온다() {
        // 서버처럼 생성기 하나를 이어서 쓴다.
        // (씨앗 0, 1, 2…로 매번 새로 만들면 java.util.Random의 첫 결과들이 서로 비슷해 치우친다)
        RandomSelection selection = new RandomSelection(new Random(42));
        Set<Long> seenInMain = new HashSet<>();
        Set<List<Long>> distinctOrders = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            TrackSelection s = selection.select(tracks, UserPreferences.empty());
            seenInMain.addAll(s.mainTrackIds());
            distinctOrders.add(s.mainTrackIds());
        }
        assertThat(seenInMain).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(distinctOrders.size()).isGreaterThan(5);
    }

    private Track track(long id, String artistName, String title, int order) {
        Artist artist = Artist.curated(artistName, britpop);
        setId(artist, id);
        Track track = Track.curated(britpop, artist, title, 1995, order < 3 ? TrackRole.MAIN : TrackRole.SPARE, order);
        setId(track, id);
        return track;
    }

    /** 테스트용: 저장하지 않은 엔티티에 ID를 넣는다 (선호 판정이 ID를 쓰기 때문) */
    private static void setId(Object entity, long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
