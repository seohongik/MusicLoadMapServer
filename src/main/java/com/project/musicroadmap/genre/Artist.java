package com.project.musicroadmap.genre;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 동명이인(같은 이름의 다른 밴드)이 있을 수 있어 유니크로 두지 않는다. 구분은 mbid로 한다 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 대표 장르 (출발점 판정용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_genre_id")
    private Genre primaryGenre;

    @Column(unique = true, length = 36)
    private String mbid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSource source;

    @Column(nullable = false)
    private boolean deprecated;

    /** 앨범 목록을 마지막으로 가져온 시각 (기획서 18장, 30일마다 갱신) */
    private LocalDateTime albumsSyncedAt;

    public static Artist curated(String name, Genre primaryGenre) {
        Artist artist = new Artist();
        artist.name = name;
        artist.primaryGenre = primaryGenre;
        artist.source = DataSource.CURATED;
        return artist;
    }

    /** MusicBrainz에서 가져온 아티스트. 계보도에 맞는 장르가 없으면 primaryGenre는 null */
    public static Artist imported(String name, String mbid, Genre primaryGenre) {
        Artist artist = new Artist();
        artist.name = name;
        artist.mbid = mbid;
        artist.primaryGenre = primaryGenre;
        artist.source = DataSource.MUSICBRAINZ;
        return artist;
    }

    /** 가져온 아티스트가 나중에 큐레이션 데이터에 들어오면, 대표 장르가 비어 있을 때만 채운다 */
    public void assignPrimaryGenreIfMissing(Genre genre) {
        if (this.primaryGenre == null) {
            this.primaryGenre = genre;
        }
    }

    public void markAlbumsSynced(LocalDateTime now) {
        this.albumsSyncedAt = now;
    }

    /** 큐레이션 아티스트에 외부 ID만 채운다. 이름·장르 등 직접 다듬은 값은 덮어쓰지 않는다 (15.2) */
    public void linkMbid(String mbid) {
        if (this.mbid == null) {
            this.mbid = mbid;
        }
    }
}
