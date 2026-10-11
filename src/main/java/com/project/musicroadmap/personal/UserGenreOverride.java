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

/**
 * 아티스트를 내가 다르게 분류한 것 (기획서 15.2 규칙 4). 공식 분류(참조 레이어)는 그대로 두고 겹쳐서 보여준다.
 * 예: 공식 분류 브릿팝 → 내 분류 인디 록 = "인디 록 (공식 분류: 브릿팝)"
 */
@Entity
@Table(name = "user_genre_override", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "artist_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGenreOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long artistId;

    @Column(nullable = false)
    private Long genreId;

    @Column(nullable = false, length = 200)
    private String note;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UserGenreOverride of(Long userId, Long artistId, Long genreId, String note, LocalDateTime now) {
        UserGenreOverride o = new UserGenreOverride();
        o.userId = userId;
        o.artistId = artistId;
        o.update(genreId, note, now);
        return o;
    }

    public void update(Long genreId, String note, LocalDateTime now) {
        this.genreId = genreId;
        this.note = note;
        this.updatedAt = now;
    }
}
