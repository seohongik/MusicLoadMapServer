package com.project.musicroadmap.personal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 내가 느낀 장르 사이의 연결 (기획서 15.3). "이 둘은 이어져 있다"는 나만의 해석 */
@Entity
@Table(name = "user_edge", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "from_genre_id", "to_genre_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEdge {

    /** 계보도 간선과 같은 기본 비용 */
    public static final double BASE_COST = 1.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long fromGenreId;

    @Column(nullable = false)
    private Long toGenreId;

    @Column(nullable = false, length = 200)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static UserEdge of(Long userId, Long fromGenreId, Long toGenreId, String note, LocalDateTime now) {
        UserEdge edge = new UserEdge();
        edge.userId = userId;
        edge.fromGenreId = fromGenreId;
        edge.toGenreId = toGenreId;
        edge.note = note;
        edge.createdAt = now;
        return edge;
    }
}
