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

/** 내가 붙인 태그 (예: "비 오는 날", "운동할 때") (기획서 15.3) */
@Entity
@Table(name = "user_tag",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PersonalTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static UserTag of(Long userId, String name, PersonalTargetType targetType, Long targetId, LocalDateTime now) {
        UserTag tag = new UserTag();
        tag.userId = userId;
        tag.name = name;
        tag.targetType = targetType;
        tag.targetId = targetId;
        tag.createdAt = now;
        return tag;
    }
}
