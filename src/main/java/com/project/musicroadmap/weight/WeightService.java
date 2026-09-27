package com.project.musicroadmap.weight;

import com.project.musicroadmap.auth.UserRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeightService {

    private final UserGenreWeightRepository weightRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserWeights load(Long userId) {
        return new UserWeights(weightRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserGenreWeight::getGenreId, UserGenreWeight::getMultiplier)));
    }

    /** 스냅샷 값으로 되돌린다: 스냅샷에 없는 장르는 기본값(행 삭제) (기획서 19장) */
    @Transactional
    public void restore(Long userId, Map<Long, Double> snapshot) {
        for (UserGenreWeight row : weightRepository.findByUserId(userId)) {
            if (!snapshot.containsKey(row.getGenreId())) {
                weightRepository.delete(row);
            }
        }
        save(userId, new UserWeights(snapshot));
    }

    /** 바뀐 값만 반영한다 (있으면 갱신, 없으면 생성) */
    @Transactional
    public void save(Long userId, UserWeights weights) {
        Map<Long, UserGenreWeight> existing = weightRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserGenreWeight::getGenreId, Function.identity()));
        weights.snapshot().forEach((genreId, multiplier) -> {
            UserGenreWeight row = existing.get(genreId);
            if (row == null) {
                weightRepository.save(UserGenreWeight.of(userRepository.getReferenceById(userId), genreId, multiplier));
            } else if (row.getMultiplier() != multiplier) {
                row.changeMultiplier(multiplier);
            }
        });
    }
}
