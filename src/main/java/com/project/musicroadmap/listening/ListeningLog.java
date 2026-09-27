package com.project.musicroadmap.listening;

import com.project.musicroadmap.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = @Index(columnList = "roadmap_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListeningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "roadmap_id", nullable = false)
    private Long roadmapId;

    @Column(nullable = false)
    private Long trackId;

    @Column(nullable = false)
    private Long genreId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Feedback feedback;

    /** feedback이 DISLIKE일 때만 값이 있다 */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DislikeReason dislikeReason;

    @Column(nullable = false, length = 100)
    private String memo;

    @Column(nullable = false)
    private LocalDateTime listenedAt;

    /** 되돌아보기(기획서 19장): 이미 남긴 반응을 고친다. 처음 들은 시각은 그대로 둔다 */
    public void update(Feedback feedback, DislikeReason dislikeReason, String memo) {
        this.feedback = feedback;
        this.dislikeReason = feedback == Feedback.DISLIKE ? dislikeReason : null;
        this.memo = memo;
    }

    public static ListeningLog of(User user, Long roadmapId, Long trackId, Long genreId, Feedback feedback,
                                  DislikeReason dislikeReason, String memo, LocalDateTime now) {
        ListeningLog log = new ListeningLog();
        log.user = user;
        log.roadmapId = roadmapId;
        log.trackId = trackId;
        log.genreId = genreId;
        log.feedback = feedback;
        log.dislikeReason = feedback == Feedback.DISLIKE ? dislikeReason : null;
        log.memo = memo;
        log.listenedAt = now;
        return log;
    }
}
