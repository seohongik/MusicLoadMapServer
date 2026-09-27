package com.project.musicroadmap.genre.importer;

import com.project.musicroadmap.auth.LoginUser;
import com.project.musicroadmap.genre.importer.ArtistImportDtos.ExternalArtistResponse;
import com.project.musicroadmap.genre.importer.ArtistImportDtos.ImportArtistRequest;
import com.project.musicroadmap.genre.importer.ArtistImportDtos.ImportArtistResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 외부 호출에는 요청 제한이 있어 로그인한 유저만 쓸 수 있게 한다 */
@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistImportController {

    private final ArtistImportService importService;

    @GetMapping("/external")
    public List<ExternalArtistResponse> searchExternal(@LoginUser Long userId, @RequestParam String q) {
        return importService.searchExternal(q);
    }

    @PostMapping("/import")
    public ImportArtistResponse importArtist(@LoginUser Long userId, @Valid @RequestBody ImportArtistRequest request) {
        return importService.importArtist(request.mbid().toLowerCase());
    }
}
