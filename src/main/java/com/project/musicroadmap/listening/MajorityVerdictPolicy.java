package com.project.musicroadmap.listening;

import com.project.musicroadmap.roadmap.domain.Verdict;
import java.util.List;
import org.springframework.stereotype.Component;

/** 다수결. MEH와 가수 이유 별로는 중립으로 센다 (가수 때문에 장르가 억울하게 판정되지 않게) */
@Component
public class MajorityVerdictPolicy implements GenreVerdictPolicy {

    @Override
    public Verdict judge(List<ListeningLog> stepLogs) {
        long likes = stepLogs.stream().filter(l -> l.getFeedback() == Feedback.LIKE).count();
        long dislikes = stepLogs.stream()
                .filter(l -> l.getFeedback() == Feedback.DISLIKE && l.getDislikeReason() != DislikeReason.ARTIST)
                .count();
        if (likes > dislikes && likes >= 2) {
            return Verdict.LIKED;
        }
        if (dislikes > likes && dislikes >= 2) {
            return Verdict.DISLIKED;
        }
        return Verdict.NEUTRAL;
    }
}
