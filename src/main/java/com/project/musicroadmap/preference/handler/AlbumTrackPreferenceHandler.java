package com.project.musicroadmap.preference.handler;

import com.project.musicroadmap.archive.AlbumTrackRepository;
import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.preference.TargetType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 앨범 수록곡 좋아요/별로 (기획서 18장). 새 대상 종류 = 이 클래스 하나 추가 */
@Component
@RequiredArgsConstructor
public class AlbumTrackPreferenceHandler implements PreferenceHandler {

    private final AlbumTrackRepository albumTrackRepository;

    @Override
    public TargetType type() {
        return TargetType.ALBUM_TRACK;
    }

    @Override
    public void validate(Long targetId) {
        if (!albumTrackRepository.existsById(targetId)) {
            throw new BusinessException(ErrorCode.TARGET_NOT_FOUND);
        }
    }

    @Override
    public String describe(Long targetId) {
        return albumTrackRepository.findByIdIn(List.of(targetId)).stream()
                .findFirst()
                .map(t -> t.getTitle() + " · " + t.getAlbum().getTitle())
                .orElse(null);
    }
}
