package com.project.musicroadmap.preference.handler;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.genre.GenreGraphProvider;
import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.TargetType;
import com.project.musicroadmap.weight.UserWeights;
import com.project.musicroadmap.weight.WeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 장르 선호는 경로 계산 가중치에도 반영한다. 선호를 지워도 가중치는 되돌리지 않는다. */
@Component
@RequiredArgsConstructor
public class GenrePreferenceHandler implements PreferenceHandler {

    private final GenreGraphProvider graphProvider;
    private final WeightService weightService;

    @Override
    public TargetType type() {
        return TargetType.GENRE;
    }

    @Override
    public void validate(Long targetId) {
        if (!graphProvider.get().contains(targetId)) {
            throw new BusinessException(ErrorCode.TARGET_NOT_FOUND);
        }
    }

    @Override
    public String describe(Long targetId) {
        GenreGraph graph = graphProvider.get();
        return graph.contains(targetId) ? graph.node(targetId).nameKo() : null;
    }

    @Override
    public void afterSaved(Long userId, Long targetId, Preference preference) {
        GenreGraph graph = graphProvider.get();
        UserWeights weights = weightService.load(userId);
        if (preference == Preference.LIKE) {
            weights.applyLike(targetId, graph);
        } else {
            weights.applyDislike(targetId, graph);
        }
        weightService.save(userId, weights);
    }
}
