package com.project.musicroadmap.archive.view;

import com.project.musicroadmap.personal.PersonalTargetType;
import java.util.List;

/**
 * 내 아카이브의 한 항목: 내가 감상·태그·좋아요를 남긴 장르, 가수, 앨범 (기획서 23장).
 *
 * @param year     시대순 보기용. 앨범은 발매 연도, 가수는 가장 이른 곡·앨범 연도, 장르는 등장 시기. 모르면 null
 * @param genreId  장르별 보기용. 가수는 내 분류가 있으면 내 분류(15.2 규칙 4), 앨범은 그 가수의 장르
 * @param rating   내 별점 (없으면 null)
 * @param liked    좋아요를 눌렀는지 (가수)
 */
public record ArchiveItem(PersonalTargetType targetType, Long targetId, String name, Integer year, Long genreId,
                          Integer rating, boolean liked, boolean reviewed, List<String> tags) {
}
