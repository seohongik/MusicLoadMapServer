package com.project.musicroadmap.relation;

import com.project.musicroadmap.auth.LoginUser;
import com.project.musicroadmap.relation.ArtistRelationService.ConnectionsResponse;
import com.project.musicroadmap.relation.ArtistRelationService.PathResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기획서 24장: 가수 관계 그래프 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ArtistRelationController {

    private final ArtistRelationService relationService;

    /** 바로 이어진 가수(멤버·협업)와 주변 가수. 처음 열 때 MusicBrainz에서 관계를 가져온다 */
    @GetMapping("/artists/{artistId}/connections")
    public ConnectionsResponse connections(@LoginUser Long userId, @PathVariable Long artistId) {
        return relationService.connections(userId, artistId);
    }

    @PostMapping("/artist-paths")
    public PathResponse bridge(@LoginUser Long userId, @Valid @RequestBody BridgeRequest request) {
        return relationService.bridge(userId, request.fromArtistId(), request.toArtistId());
    }

    public record BridgeRequest(@NotNull Long fromArtistId, @NotNull Long toArtistId) {
    }
}
