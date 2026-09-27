package com.project.musicroadmap.genre;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 참조 레이어 (기획서 15장): 유저 요청으로 수정하지 않는다 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 코드에서 참조하기 위한 고정 키 (예: BRITPOP) */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 60)
    private String nameKo;

    @Column(nullable = false)
    private int originDecade;

    @Column(nullable = false, length = 500)
    private String description;

    /** 계보도 좌표 (dp) */
    private int posX;

    private int posY;

    @Column(unique = true, length = 36)
    private String mbid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSource source;

    @Column(nullable = false)
    private boolean deprecated;

    public static Genre curated(String code, String name, String nameKo, int originDecade,
                                String description, int posX, int posY) {
        Genre genre = new Genre();
        genre.code = code;
        genre.name = name;
        genre.nameKo = nameKo;
        genre.originDecade = originDecade;
        genre.description = description;
        genre.posX = posX;
        genre.posY = posY;
        genre.source = DataSource.CURATED;
        return genre;
    }

    /** 큐레이션 데이터 파일의 값으로 맞춘다. 바뀐 것이 있으면 true (직접 만든 CURATED 장르만) */
    public boolean syncFromSeed(String name, String nameKo, int originDecade, String description, int posX, int posY) {
        if (source != DataSource.CURATED) {
            return false;
        }
        boolean changed = !name.equals(this.name) || !nameKo.equals(this.nameKo) || originDecade != this.originDecade
                || !description.equals(this.description) || posX != this.posX || posY != this.posY;
        this.name = name;
        this.nameKo = nameKo;
        this.originDecade = originDecade;
        this.description = description;
        this.posX = posX;
        this.posY = posY;
        return changed;
    }
}
