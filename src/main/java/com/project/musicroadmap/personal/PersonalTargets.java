package com.project.musicroadmap.personal;

import com.project.musicroadmap.archive.Album;
import com.project.musicroadmap.archive.AlbumRepository;
import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 개인 레이어가 가리키는 참조 레이어 대상을 확인하고 이름을 붙인다 (참조 레이어는 읽기만) */
@Component
@RequiredArgsConstructor
class PersonalTargets {

    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;

    /** 없으면 TARGET_NOT_FOUND */
    String nameOf(PersonalTargetType type, Long id) {
        return switch (type) {
            case GENRE -> genreRepository.findById(id).map(g -> g.getNameKo()).orElseThrow(PersonalTargets::notFound);
            case ARTIST -> artistRepository.findById(id).map(a -> a.getName()).orElseThrow(PersonalTargets::notFound);
            case ALBUM -> albumRepository.findWithArtistById(id).map(PersonalTargets::albumName)
                    .orElseThrow(PersonalTargets::notFound);
        };
    }

    /** 목록용: 대상이 사라졌어도(있을 수 없지만) 목록 전체가 깨지지 않게 한다 */
    String nameOrBlank(PersonalTargetType type, Long id) {
        try {
            return nameOf(type, id);
        } catch (BusinessException e) {
            return "";
        }
    }

    private static String albumName(Album album) {
        return album.getTitle() + " · " + album.getArtist().getName();
    }

    private static BusinessException notFound() {
        return new BusinessException(ErrorCode.TARGET_NOT_FOUND);
    }
}
