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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private int releaseYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TrackRole role;

    /** 장르 안에서의 순서 */
    @Column(nullable = false)
    private int displayOrder;

    @Column(unique = true, length = 36)
    private String mbid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSource source;

    @Column(nullable = false)
    private boolean deprecated;

    public static Track curated(Genre genre, Artist artist, String title, int releaseYear, TrackRole role, int displayOrder) {
        Track track = new Track();
        track.genre = genre;
        track.artist = artist;
        track.title = title;
        track.releaseYear = releaseYear;
        track.role = role;
        track.displayOrder = displayOrder;
        track.source = DataSource.CURATED;
        return track;
    }

    /** 유튜브 검색어 */
    public String searchQuery() {
        return artist.getName() + " " + title;
    }
}
