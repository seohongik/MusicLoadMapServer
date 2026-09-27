package com.project.musicroadmap.genre;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장르의 핵심 가수와 "이 장르에서의 역할" 소개 (기획서 17장).
 * 한 가수가 여러 장르에 나올 수 있고 장르마다 소개가 다르다 (예: Bob Dylan - 포크 / 포크 록).
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"genre_id", "artist_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenreArtist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @Column(nullable = false, length = 300)
    private String intro;

    /** 장르 안에서의 순서 */
    @Column(nullable = false)
    private int displayOrder;

    /** 큐레이션 데이터 파일의 소개·순서로 맞춘다. 바뀐 것이 있으면 true */
    public boolean syncFromSeed(String intro, int displayOrder) {
        boolean changed = !intro.equals(this.intro) || displayOrder != this.displayOrder;
        this.intro = intro;
        this.displayOrder = displayOrder;
        return changed;
    }

    public static GenreArtist of(Genre genre, Artist artist, String intro, int displayOrder) {
        GenreArtist genreArtist = new GenreArtist();
        genreArtist.genre = genre;
        genreArtist.artist = artist;
        genreArtist.intro = intro;
        genreArtist.displayOrder = displayOrder;
        return genreArtist;
    }
}
