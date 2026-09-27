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

/** 기원 장르(from) → 파생 장르(to) */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"from_genre_id", "to_genre_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenreEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_genre_id")
    private Genre fromGenre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_genre_id")
    private Genre toGenre;

    @Column(nullable = false)
    private double baseCost;

    public static GenreEdge of(Genre from, Genre to) {
        GenreEdge edge = new GenreEdge();
        edge.fromGenre = from;
        edge.toGenre = to;
        edge.baseCost = 1.0;
        return edge;
    }
}
