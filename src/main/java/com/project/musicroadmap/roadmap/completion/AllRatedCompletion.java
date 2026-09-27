package com.project.musicroadmap.roadmap.completion;

import com.project.musicroadmap.listening.GenreVerdictPolicy;
import com.project.musicroadmap.listening.ListeningLog;
import com.project.musicroadmap.roadmap.domain.RoadmapStep;
import com.project.musicroadmap.roadmap.domain.Verdict;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 초기 방식: 배정된 곡에 모두 반응하면 다수결로 자동 판정. 버튼으로는 넘어갈 수 없다 */
@Component
@RequiredArgsConstructor
public class AllRatedCompletion implements StepCompletionPolicy {

    private final GenreVerdictPolicy verdictPolicy;

    @Override
    public StepCompletionType type() {
        return StepCompletionType.ALL_RATED;
    }

    @Override
    public Optional<Verdict> autoVerdict(RoadmapStep step, List<ListeningLog> stepLogs) {
        Set<Long> rated = stepLogs.stream().map(ListeningLog::getTrackId).collect(Collectors.toSet());
        if (step.getTrackIds().isEmpty() || !rated.containsAll(step.getTrackIds())) {
            return Optional.empty();
        }
        return Optional.of(verdictPolicy.judge(stepLogs));
    }

    @Override
    public boolean allowsManualNext() {
        return false;
    }
}
