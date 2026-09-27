package com.project.musicroadmap.weight;

import com.project.musicroadmap.auth.User;
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

/** 개인 레이어 (기획서 15장). 행이 없으면 가중치 1.0 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "genre_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGenreWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "genre_id", nullable = false)
    private Long genreId;

    @Column(nullable = false)
    private double multiplier;

    public static UserGenreWeight of(User user, Long genreId, double multiplier) {
        UserGenreWeight weight = new UserGenreWeight();
        weight.user = user;
        weight.genreId = genreId;
        weight.multiplier = multiplier;
        return weight;
    }

    public void changeMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }
}
