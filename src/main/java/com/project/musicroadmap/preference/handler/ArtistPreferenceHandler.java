package com.project.musicroadmap.preference.handler;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.preference.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 가수 선호는 스텝이 시작될 때 곡 고르기에 반영된다 (TrackSelectionStrategy) */
@Component
@RequiredArgsConstructor
public class ArtistPreferenceHandler implements PreferenceHandler {

    private final ArtistRepository artistRepository;

    @Override
    public TargetType type() {
        return TargetType.ARTIST;
    }

    @Override
    public void validate(Long targetId) {
        if (!artistRepository.existsById(targetId)) {
            throw new BusinessException(ErrorCode.TARGET_NOT_FOUND);
        }
    }

    @Override
    public String describe(Long targetId) {
        return artistRepository.findById(targetId).map(a -> a.getName()).orElse(null);
    }
}
