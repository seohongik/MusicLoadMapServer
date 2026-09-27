package com.project.musicroadmap.genre;

import com.project.musicroadmap.genre.CatalogDtos.ArtistDto;
import com.project.musicroadmap.genre.CatalogDtos.CatalogResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 참조 레이어 조회. 로그인 없이 볼 수 있다. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/catalog")
    public CatalogResponse catalog() {
        return catalogService.getCatalog();
    }

    @GetMapping("/artists")
    public List<ArtistDto> searchArtists(@RequestParam(defaultValue = "") String q) {
        return catalogService.searchArtists(q);
    }
}
