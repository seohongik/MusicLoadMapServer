package com.project.musicroadmap.personal;

import com.project.musicroadmap.auth.LoginUser;
import com.project.musicroadmap.personal.PersonalDtos.GenreOverrideRequest;
import com.project.musicroadmap.personal.PersonalDtos.GenreOverrideResponse;
import com.project.musicroadmap.personal.PersonalDtos.MyNotes;
import com.project.musicroadmap.personal.PersonalDtos.ReviewRequest;
import com.project.musicroadmap.personal.PersonalDtos.ReviewResponse;
import com.project.musicroadmap.personal.PersonalDtos.TagRequest;
import com.project.musicroadmap.personal.PersonalDtos.TagSummary;
import com.project.musicroadmap.personal.PersonalDtos.TaggedTarget;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 개인 레이어: 감상, 태그, 나만의 분류 (기획서 15장, 22장) */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class PersonalController {

    private final PersonalService personalService;

    /** 대상 하나에 남긴 내 감상과 태그 (장르 상세 화면용) */
    @GetMapping("/notes/{targetType}/{targetId}")
    public MyNotes notes(@LoginUser Long userId, @PathVariable PersonalTargetType targetType, @PathVariable Long targetId) {
        return personalService.notesOf(userId, targetType, targetId);
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> reviews(@LoginUser Long userId) {
        return personalService.reviews(userId);
    }

    @PutMapping("/reviews")
    public ReviewResponse saveReview(@LoginUser Long userId, @Valid @RequestBody ReviewRequest request) {
        return personalService.saveReview(userId, request);
    }

    @DeleteMapping("/reviews/{targetType}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@LoginUser Long userId, @PathVariable PersonalTargetType targetType,
                             @PathVariable Long targetId) {
        personalService.deleteReview(userId, targetType, targetId);
    }

    @GetMapping("/tags")
    public List<TagSummary> tags(@LoginUser Long userId) {
        return personalService.tags(userId);
    }

    /** 태그 이름은 공백·한글이 들어가서 쿼리 파라미터로 받는다 */
    @GetMapping("/tags/targets")
    public List<TaggedTarget> tagged(@LoginUser Long userId, @RequestParam String name) {
        return personalService.tagged(userId, name);
    }

    /** 대상에 붙은 태그 목록을 돌려준다 */
    @PutMapping("/tags")
    public List<String> addTag(@LoginUser Long userId, @Valid @RequestBody TagRequest request) {
        return personalService.addTag(userId, request);
    }

    @DeleteMapping("/tags/{targetType}/{targetId}")
    public List<String> removeTag(@LoginUser Long userId, @PathVariable PersonalTargetType targetType,
                                  @PathVariable Long targetId, @RequestParam String name) {
        return personalService.removeTag(userId, targetType, targetId, name);
    }

    @GetMapping("/genre-overrides")
    public List<GenreOverrideResponse> overrides(@LoginUser Long userId) {
        return personalService.overrides(userId);
    }

    @PutMapping("/genre-overrides")
    public GenreOverrideResponse saveOverride(@LoginUser Long userId, @Valid @RequestBody GenreOverrideRequest request) {
        return personalService.saveOverride(userId, request);
    }

    @DeleteMapping("/genre-overrides/{artistId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOverride(@LoginUser Long userId, @PathVariable Long artistId) {
        personalService.deleteOverride(userId, artistId);
    }
}
