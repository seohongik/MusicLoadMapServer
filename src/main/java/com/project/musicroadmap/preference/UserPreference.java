package com.project.musicroadmap.preference;

import com.project.musicroadmap.auth.User;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기획서 16.1. targetId는 대상 종류마다 가리키는 테이블이 달라 외래키를 걸지 않는다.
 * 대상 존재 여부는 PreferenceHandler가 검증한다.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Preference preference;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UserPreference of(User user, TargetType targetType, Long targetId, Preference preference, LocalDateTime now) {
        UserPreference p = new UserPreference();
        p.user = user;
        p.targetType = targetType;
        p.targetId = targetId;
        p.preference = preference;
        p.updatedAt = now;
        return p;
    }

    public void change(Preference preference, LocalDateTime now) {
        this.preference = preference;
        this.updatedAt = now;
    }
}
