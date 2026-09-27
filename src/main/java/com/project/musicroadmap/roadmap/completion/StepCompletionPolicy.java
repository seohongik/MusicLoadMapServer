package com.project.musicroadmap.roadmap.completion;

import com.project.musicroadmap.listening.ListeningLog;
import com.project.musicroadmap.roadmap.domain.RoadmapStep;
import com.project.musicroadmap.roadmap.domain.Verdict;
import java.util.List;
import java.util.Optional;

public interface StepCompletionPolicy {

    StepCompletionType type();

    /** 반응이 들어올 때마다 호출. 값이 있으면 그 판정으로 스텝을 자동 완료한다 */
    Optional<Verdict> autoVerdict(RoadmapStep step, List<ListeningLog> stepLogs);

    /** "다음 장르로" 버튼을 허용하는지 */
    boolean allowsManualNext();
}
