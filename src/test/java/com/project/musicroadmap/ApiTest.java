package com.project.musicroadmap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.project.musicroadmap.genre.GenreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** HTTP 계층: 인증, 에러 형식(기획서 9장), 요청 검증 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired GenreRepository genreRepository;

    private String login(String nickname) throws Exception {
        String body = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private Long genre(String code) {
        return genreRepository.findByCode(code).orElseThrow().getId();
    }

    @Test
    void 카탈로그는_로그인_없이_받는다() throws Exception {
        mockMvc.perform(get("/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres.length()").value(28))
                .andExpect(jsonPath("$.edges.length()").value(43))
                .andExpect(jsonPath("$.tracks.length()").value(112))
                // 기획서 17장: 소개 중심, 기본 재생 방식은 NONE
                .andExpect(jsonPath("$.playbackType").value("NONE"))
                .andExpect(jsonPath("$.genreArtists.length()").value(112))
                .andExpect(jsonPath("$.genreArtists[0].intro").isNotEmpty())
                .andExpect(jsonPath("$.tracks[0].listenUrl").doesNotExist());
    }

    @Test
    void 토큰이_없으면_401과_에러_형식() throws Exception {
        mockMvc.perform(get("/api/today"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void 로드맵_생성과_중복_생성_거절() throws Exception {
        String token = login("api-tester");
        String request = """
                {"explorationType":"BRIDGE","startGenreId":%d,"targetGenreId":%d,"maxSteps":5}
                """.formatted(genre("BRITPOP"), genre("HIP_HOP"));

        mockMvc.perform(post("/api/roadmaps").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.steps.length()").value(5))
                .andExpect(jsonPath("$.currentStepIndex").value(1));

        mockMvc.perform(post("/api/roadmaps").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROADMAP_ALREADY_IN_PROGRESS"));

        mockMvc.perform(get("/api/today").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackIds.length()").value(3))
                .andExpect(jsonPath("$.genresCompletedToday").value(0));
    }

    @Test
    void 잘못된_enum_값은_400() throws Exception {
        String token = login("api-tester2");
        mockMvc.perform(post("/api/roadmaps").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"explorationType\":\"WRONG\",\"planType\":\"TASTING\",\"startGenreId\":1,\"maxSteps\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 미리보기는_전략별_경로를_나란히_준다() throws Exception {
        String token = login("api-tester3");
        mockMvc.perform(post("/api/roadmaps/preview").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startGenreId\":%d,\"maxSteps\":5}".formatted(genre("BLUES"))))
                .andExpect(status().isOk())
                // 목표 장르가 없으면 두 장르 잇기를 뺀 4가지. 새 탐험 방식은 컨트롤러 수정 없이 자동으로 들어온다 (OCP)
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].explorationType").value("ROOTS"))
                .andExpect(jsonPath("$[0].errorCode").value("DEAD_END"))
                .andExpect(jsonPath("$[1].explorationType").value("DESCENDANTS"))
                .andExpect(jsonPath("$[2].explorationType").value("NEIGHBORS"))
                .andExpect(jsonPath("$[2].errorCode").value("DEAD_END"))      // 블루스는 부모가 없어 형제도 없음
                .andExpect(jsonPath("$[3].explorationType").value("CHRONOLOGICAL"))
                .andExpect(jsonPath("$[3].path.length()").value(6));
    }
}
