package com.project.musicroadmap.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** MVP 개발용 로그인. 운영(prod) 프로필에서는 등록되지 않는다. */
@Profile("!prod")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/dev-login")
    public DevLoginResponse devLogin(@Valid @RequestBody DevLoginRequest request) {
        User user = authService.devLogin(request.nickname().trim());
        return new DevLoginResponse(user.getId(), user.getNickname(), user.getAccessToken());
    }

    public record DevLoginRequest(@NotBlank @Size(max = 30) String nickname) {
    }

    public record DevLoginResponse(Long userId, String nickname, String accessToken) {
    }
}
