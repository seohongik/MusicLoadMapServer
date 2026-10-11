package com.project.musicroadmap.personal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class PersonalDtos {

    private PersonalDtos() {
    }

    public record ReviewRequest(@NotNull PersonalTargetType targetType, @NotNull Long targetId,
                                @Min(1) @Max(5) Integer rating, @NotBlank @Size(max = 1000) String content) {
    }

    /** targetName: 목록에서 보여줄 대상 이름 (예: "Oasis", "Definitely Maybe · Oasis") */
    public record ReviewResponse(PersonalTargetType targetType, Long targetId, String targetName, Integer rating,
                                 String content, LocalDateTime updatedAt) {
        static ReviewResponse of(Review r, String targetName) {
            return new ReviewResponse(r.getTargetType(), r.getTargetId(), targetName, r.getRating(), r.getContent(),
                    r.getUpdatedAt());
        }
    }

    public record TagRequest(@NotNull PersonalTargetType targetType, @NotNull Long targetId,
                             @NotBlank @Size(max = 20) String name) {
    }

    public record TagSummary(String name, int count) {
    }

    public record TaggedTarget(PersonalTargetType targetType, Long targetId, String targetName) {
    }

    public record GenreOverrideRequest(@NotNull Long artistId, @NotNull Long genreId, @Size(max = 200) String note) {
    }

    /** 공식 분류(officialGenreId)와 내 분류(genreId)를 함께 준다 (15.2 규칙 4) */
    public record GenreOverrideResponse(Long artistId, String artistName, Long officialGenreId, Long genreId,
                                        String note) {
    }

    /** 가수·앨범 상세에 겹쳐서 보여줄 내 기록 */
    public record MyNotes(ReviewResponse review, List<String> tags) {
    }
}
