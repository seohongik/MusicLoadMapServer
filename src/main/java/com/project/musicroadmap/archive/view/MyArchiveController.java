package com.project.musicroadmap.archive.view;

import com.project.musicroadmap.archive.view.MyArchiveService.ArchiveViewResponse;
import com.project.musicroadmap.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 기획서 23장: 내 아카이브를 보기 방식(전략)대로 묶어서 준다 */
@RestController
@RequestMapping("/api/me/archive")
@RequiredArgsConstructor
public class MyArchiveController {

    private final MyArchiveService archiveService;

    @GetMapping
    public ArchiveViewResponse archive(@LoginUser Long userId,
                                       @RequestParam(defaultValue = "TIMELINE") ArchiveViewType view) {
        return archiveService.view(userId, view);
    }
}
