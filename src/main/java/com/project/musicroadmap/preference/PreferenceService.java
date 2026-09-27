package com.project.musicroadmap.preference;

import com.project.musicroadmap.auth.UserRepository;
import com.project.musicroadmap.common.AppClock;
import com.project.musicroadmap.preference.handler.PreferenceHandler;
import com.project.musicroadmap.preference.handler.PreferenceHandlerResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final UserPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final PreferenceHandlerResolver handlerResolver;
    private final AppClock clock;

    @Transactional(readOnly = true)
    public List<PreferenceResponse> list(Long userId) {
        return preferenceRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(p -> PreferenceResponse.from(p, handlerResolver.get(p.getTargetType()).describe(p.getTargetId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserPreferences load(Long userId) {
        return new UserPreferences(preferenceRepository.findByUserIdOrderByUpdatedAtDesc(userId));
    }

    /** 같은 대상에 다시 저장하면 덮어쓴다 */
    @Transactional
    public PreferenceResponse save(Long userId, TargetType targetType, Long targetId, Preference preference) {
        PreferenceHandler handler = handlerResolver.get(targetType);
        handler.validate(targetId);

        UserPreference saved = preferenceRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .map(existing -> {
                    existing.change(preference, clock.now());
                    return existing;
                })
                .orElseGet(() -> preferenceRepository.save(UserPreference.of(
                        userRepository.getReferenceById(userId), targetType, targetId, preference, clock.now())));

        handler.afterSaved(userId, targetId, preference);
        return PreferenceResponse.from(saved, handler.describe(targetId));
    }

    @Transactional
    public void delete(Long userId, TargetType targetType, Long targetId) {
        preferenceRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .ifPresent(preferenceRepository::delete);
    }

    /** targetName: 화면에 보여줄 대상 이름 (앨범 수록곡처럼 앱이 모르는 대상도 있어서 서버가 준다) */
    public record PreferenceResponse(TargetType targetType, Long targetId, Preference preference, String targetName) {
        static PreferenceResponse from(UserPreference p, String targetName) {
            return new PreferenceResponse(p.getTargetType(), p.getTargetId(), p.getPreference(), targetName);
        }
    }
}
