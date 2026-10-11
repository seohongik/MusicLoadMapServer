package com.project.musicroadmap.personal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 앨범·아티스트·장르 단위 감상 (기획서 15.3). 대상마다 하나, 고쳐 쓸 수 있다 */
@Entity
@Table(name = "user_review",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PersonalTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    /** 1~5, 없어도 된다 */
    private Integer rating;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Review of(Long userId, PersonalTargetType targetType, Long targetId, Integer rating, String content,
                            LocalDateTime now) {
        Review review = new Review();
        review.userId = userId;
        review.targetType = targetType;
        review.targetId = targetId;
        review.rating = rating;
        review.content = content;
        review.createdAt = now;
        review.updatedAt = now;
        return review;
    }

    public void update(Integer rating, String content, LocalDateTime now) {
        this.rating = rating;
        this.content = content;
        this.updatedAt = now;
    }
}
