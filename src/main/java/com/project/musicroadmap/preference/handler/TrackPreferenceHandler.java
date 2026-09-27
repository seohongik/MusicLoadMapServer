package com.project.musicroadmap.preference.handler;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.TrackRepository;
import com.project.musicroadmap.preference.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackPreferenceHandler implements PreferenceHandler {

    private final TrackRepository trackRepository;

    @Override
    public TargetType type() {
        return TargetType.TRACK;
    }

    @Override
    public void validate(Long targetId) {
        if (!trackRepository.existsById(targetId)) {
            throw new BusinessException(ErrorCode.TARGET_NOT_FOUND);
        }
    }

    @Override
    public String describe(Long targetId) {
        return trackRepository.findWithArtistById(targetId)
                .map(t -> t.getTitle() + " · " + t.getArtist().getName())
                .orElse(null);
    }
}
