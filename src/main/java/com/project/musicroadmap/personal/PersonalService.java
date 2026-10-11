package com.project.musicroadmap.personal;

import com.project.musicroadmap.common.AppClock;
import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.GenreRepository;
import com.project.musicroadmap.personal.PersonalDtos.GenreOverrideRequest;
import com.project.musicroadmap.personal.PersonalDtos.GenreOverrideResponse;
import com.project.musicroadmap.personal.PersonalDtos.MyNotes;
import com.project.musicroadmap.personal.PersonalDtos.ReviewRequest;
import com.project.musicroadmap.personal.PersonalDtos.ReviewResponse;
import com.project.musicroadmap.personal.PersonalDtos.TagRequest;
import com.project.musicroadmap.personal.PersonalDtos.TagSummary;
import com.project.musicroadmap.personal.PersonalDtos.TaggedTarget;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인 레이어: 감상, 태그, 나만의 분류 (기획서 15장).
 * 참조 레이어는 읽기만 하고, 모든 조회에 userId 조건을 건다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PersonalService {

    private final ReviewRepository reviewRepository;
    private final UserTagRepository tagRepository;
    private final UserGenreOverrideRepository overrideRepository;
    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final PersonalTargets targets;
    private final AppClock clock;

    // --- 감상 ---

    public ReviewResponse saveReview(Long userId, ReviewRequest request) {
        String name = targets.nameOf(request.targetType(), request.targetId());
        String content = request.content().trim();
        Review review = reviewRepository.findByUserIdAndTargetTypeAndTargetId(userId, request.targetType(), request.targetId())
                .map(r -> {
                    r.update(request.rating(), content, clock.now());
                    return r;
                })
                .orElseGet(() -> reviewRepository.save(Review.of(userId, request.targetType(), request.targetId(),
                        request.rating(), content, clock.now())));
        return ReviewResponse.of(review, name);
    }

    public void deleteReview(Long userId, PersonalTargetType type, Long targetId) {
        reviewRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId).ifPresent(reviewRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> reviews(Long userId) {
        return reviewRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(r -> ReviewResponse.of(r, targets.nameOrBlank(r.getTargetType(), r.getTargetId())))
                .toList();
    }

    // --- 태그 ---

    /** "#비 오는  날" → "비 오는 날" */
    static String normalizeTag(String name) {
        String trimmed = name.trim().replaceAll("\\s+", " ");
        while (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return trimmed;
    }

    public List<String> addTag(Long userId, TagRequest request) {
        targets.nameOf(request.targetType(), request.targetId());
        String name = normalizeTag(request.name());
        if (tagRepository.findByUserIdAndTargetTypeAndTargetIdAndName(userId, request.targetType(), request.targetId(), name).isEmpty()) {
            tagRepository.save(UserTag.of(userId, name, request.targetType(), request.targetId(), clock.now()));
        }
        return tagsOf(userId, request.targetType(), request.targetId());
    }

    public List<String> removeTag(Long userId, PersonalTargetType type, Long targetId, String name) {
        tagRepository.findByUserIdAndTargetTypeAndTargetIdAndName(userId, type, targetId, normalizeTag(name))
                .ifPresent(tagRepository::delete);
        return tagsOf(userId, type, targetId);
    }

    /** 내 태그와 붙인 횟수 (처음 붙인 순서) */
    @Transactional(readOnly = true)
    public List<TagSummary> tags(Long userId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        tagRepository.findByUserIdOrderByCreatedAtAsc(userId).forEach(t -> counts.merge(t.getName(), 1, Integer::sum));
        return counts.entrySet().stream().map(e -> new TagSummary(e.getKey(), e.getValue())).toList();
    }

    @Transactional(readOnly = true)
    public List<TaggedTarget> tagged(Long userId, String name) {
        return tagRepository.findByUserIdAndNameOrderByCreatedAtDesc(userId, normalizeTag(name)).stream()
                .map(t -> new TaggedTarget(t.getTargetType(), t.getTargetId(),
                        targets.nameOrBlank(t.getTargetType(), t.getTargetId())))
                .toList();
    }

    // --- 나만의 분류 ---

    public GenreOverrideResponse saveOverride(Long userId, GenreOverrideRequest request) {
        Artist artist = artistRepository.findById(request.artistId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TARGET_NOT_FOUND));
        genreRepository.findById(request.genreId()).orElseThrow(() -> new BusinessException(ErrorCode.GENRE_NOT_FOUND));
        String note = request.note() == null ? "" : request.note().trim();
        UserGenreOverride override = overrideRepository.findByUserIdAndArtistId(userId, artist.getId())
                .map(o -> {
                    o.update(request.genreId(), note, clock.now());
                    return o;
                })
                .orElseGet(() -> overrideRepository.save(
                        UserGenreOverride.of(userId, artist.getId(), request.genreId(), note, clock.now())));
        return toResponse(override, artist);
    }

    public void deleteOverride(Long userId, Long artistId) {
        overrideRepository.findByUserIdAndArtistId(userId, artistId).ifPresent(overrideRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<GenreOverrideResponse> overrides(Long userId) {
        return overrideRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(o -> toResponse(o, artistRepository.findById(o.getArtistId()).orElse(null)))
                .toList();
    }

    // --- 상세 화면에 겹쳐 보여줄 내 기록 (15.4) ---

    @Transactional(readOnly = true)
    public MyNotes notesOf(Long userId, PersonalTargetType type, Long targetId) {
        ReviewResponse review = reviewRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId)
                .map(r -> ReviewResponse.of(r, ""))
                .orElse(null);
        return new MyNotes(review, tagsOf(userId, type, targetId));
    }

    @Transactional(readOnly = true)
    public Optional<GenreOverrideResponse> overrideOf(Long userId, Long artistId) {
        return overrideRepository.findByUserIdAndArtistId(userId, artistId)
                .map(o -> toResponse(o, artistRepository.findById(artistId).orElse(null)));
    }

    private List<String> tagsOf(Long userId, PersonalTargetType type, Long targetId) {
        return tagRepository.findByUserIdAndTargetTypeAndTargetIdOrderByCreatedAtAsc(userId, type, targetId).stream()
                .map(UserTag::getName)
                .toList();
    }

    private static GenreOverrideResponse toResponse(UserGenreOverride o, Artist artist) {
        Long official = artist == null || artist.getPrimaryGenre() == null ? null : artist.getPrimaryGenre().getId();
        return new GenreOverrideResponse(o.getArtistId(), artist == null ? "" : artist.getName(), official,
                o.getGenreId(), o.getNote());
    }
}
