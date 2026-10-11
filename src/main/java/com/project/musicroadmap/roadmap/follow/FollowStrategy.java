package com.project.musicroadmap.roadmap.follow;

import com.project.musicroadmap.weight.UserWeights;
import java.util.List;

/** 같은 공유 로드맵이라도 따라가는 유저의 취향에 따라 결과가 달라진다 (기획서 21장) */
public interface FollowStrategy {

    FollowMode mode();

    /** 공유된 경로(출발 장르 포함)에서 내가 걸을 경로를 만든다. 출발 장르는 항상 남긴다 */
    List<Long> startPath(List<Long> sharedPath, UserWeights myWeights);

    /** 판정(좋아함·싫어함) 뒤에 남은 경로를 다시 짤지 */
    boolean recalculatesOnVerdict();
}
