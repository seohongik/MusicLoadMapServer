package com.project.musicroadmap.roadmap.completion;

import com.project.musicroadmap.listening.ListeningLog;
import com.project.musicroadmap.roadmap.domain.RoadmapStep;
import com.project.musicroadmap.roadmap.domain.Verdict;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 유저가 "다음 장르로"를 누를 때 끝낸다. 반응만으로는 넘어가지 않는다 */
@Component
public class ManualNextCompletion implements StepCompletionPolicy {

    @Override
    public StepCompletionType type() {
        return StepCompletionType.MANUAL_NEXT;
    }

    @Override
    public Optional<Verdict> autoVerdict(RoadmapStep step, List<ListeningLog> stepLogs) {
        return Optional.empty();
    }

    @Override
    public boolean allowsManualNext() {
        return true;
    }
}
