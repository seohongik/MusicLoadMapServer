package com.project.musicroadmap.genre.mapping;

import com.project.musicroadmap.genre.Genre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** MusicBrainz 장르명(소문자) → 우리 계보도의 장르 (기획서 14.2.1) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenreAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String alias;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id")
    private Genre genre;

    /** 큐레이션 데이터 파일에서 별칭이 다른 장르로 옮겨 갔을 때 (예: synth-pop 뉴 웨이브 → 신스팝) */
    public boolean pointTo(Genre genre) {
        if (this.genre.getId().equals(genre.getId())) {
            return false;
        }
        this.genre = genre;
        return true;
    }

    public static GenreAlias of(String alias, Genre genre) {
        GenreAlias genreAlias = new GenreAlias();
        genreAlias.alias = alias.toLowerCase();
        genreAlias.genre = genre;
        return genreAlias;
    }
}
