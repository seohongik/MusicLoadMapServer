package com.project.musicroadmap.archive;

import com.project.musicroadmap.archive.ArchiveDtos.AlbumDetailResponse;
import com.project.musicroadmap.archive.ArchiveDtos.ArtistDetailResponse;
import com.project.musicroadmap.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 가수 상세 → 앨범 상세 (기획서 18장). 곡별 좋아요/별로는 PUT /api/me/preferences (targetType=ALBUM_TRACK) */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @GetMapping("/artists/{artistId}")
    public ArtistDetailResponse artist(@LoginUser Long userId, @PathVariable Long artistId) {
        return archiveService.getArtistDetail(userId, artistId);
    }

    @GetMapping("/albums/{albumId}")
    public AlbumDetailResponse album(@LoginUser Long userId, @PathVariable Long albumId) {
        return archiveService.getAlbumDetail(userId, albumId);
    }
}
