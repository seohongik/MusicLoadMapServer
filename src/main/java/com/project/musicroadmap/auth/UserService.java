package com.project.musicroadmap.auth;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.roadmap.selection.TrackSelectionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TrackSelectionType getTrackSelectionType(Long userId) {
        return userRepository.findById(userId)
                .map(User::getTrackSelectionType)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    @Transactional
    public TrackSelectionType changeTrackSelectionType(Long userId, TrackSelectionType type) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        user.changeTrackSelectionType(type);
        return user.getTrackSelectionType();
    }
}
