package com.project.musicroadmap.auth;

import com.project.musicroadmap.common.AppClock;
import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.roadmap.selection.TrackSelectionType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AppClock clock;

    /** 새 유저의 곡 고르기 방식 (기본 RANDOM) */
    @Value("${app.track-selection.default:RANDOM}")
    private TrackSelectionType defaultSelection;

    /** 닉네임으로 유저를 찾고, 없으면 만든다 (개발용) */
    @Transactional
    public User devLogin(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseGet(() -> userRepository.save(User.create(nickname, clock.now(), defaultSelection)));
    }

    @Transactional(readOnly = true)
    public Long authenticate(String accessToken) {
        return userRepository.findByAccessToken(accessToken)
                .map(User::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
