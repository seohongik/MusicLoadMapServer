package com.project.musicroadmap.auth;

import com.project.musicroadmap.roadmap.selection.TrackSelectionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    /** MVP 개발용 로그인 토큰. 구글 로그인 도입 시 JWT로 바꾼다 (기획서 9장) */
    @Column(nullable = false, unique = true, length = 64)
    private String accessToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrackSelectionType trackSelectionType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static User create(String nickname, LocalDateTime now, TrackSelectionType defaultSelection) {
        User user = new User();
        user.nickname = nickname;
        user.accessToken = UUID.randomUUID().toString();
        user.trackSelectionType = defaultSelection;
        user.createdAt = now;
        return user;
    }

    public void changeTrackSelectionType(TrackSelectionType type) {
        this.trackSelectionType = type;
    }
}
