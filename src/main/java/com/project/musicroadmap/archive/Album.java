package com.project.musicroadmap.archive;

import com.project.musicroadmap.genre.Artist;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정규 앨범 (참조 레이어, 기획서 18장). MusicBrainz release-group 하나에 대응한다.
 * 수록곡은 처음 열 때 가져온다 (tracksSyncedAt이 null이면 아직 안 가져옴).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    /** MusicBrainz release-group ID */
    @Column(nullable = false, unique = true, length = 36)
    private String mbid;

    @Column(nullable = false, length = 500)
    private String title;

    private Integer releaseYear;

    /** 수록곡을 가져온 발매판 (가장 이른 공식 발매판) */
    @Column(length = 36)
    private String releaseMbid;

    private LocalDateTime tracksSyncedAt;

    public static Album of(Artist artist, String mbid, String title, Integer releaseYear) {
        Album album = new Album();
        album.artist = artist;
        album.mbid = mbid;
        album.title = title;
        album.releaseYear = releaseYear;
        return album;
    }

    public void update(String title, Integer releaseYear) {
        this.title = title;
        this.releaseYear = releaseYear;
    }

    public void markTracksSynced(String releaseMbid, LocalDateTime now) {
        this.releaseMbid = releaseMbid;
        this.tracksSyncedAt = now;
    }

    public boolean tracksSynced() {
        return tracksSyncedAt != null;
    }

    /** 커버는 저장하지 않고 링크로만 보여준다 (기획서 14.1) */
    public String coverUrl() {
        return "https://coverartarchive.org/release-group/" + mbid + "/front-250";
    }
}
