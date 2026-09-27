package com.project.musicroadmap.listening;

import com.project.musicroadmap.roadmap.domain.Verdict;
import java.util.List;

/** 기획서 5.4 장르 판정 */
public interface GenreVerdictPolicy {

    Verdict judge(List<ListeningLog> stepLogs);
}
