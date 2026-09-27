package com.project.musicroadmap.roadmap.selection;

import java.util.List;

/** 스텝에서 들을 대표곡(최대 3곡)과 애매함일 때 줄 예비곡(없을 수 있음) */
public record TrackSelection(List<Long> mainTrackIds, Long spareTrackId) {
}
