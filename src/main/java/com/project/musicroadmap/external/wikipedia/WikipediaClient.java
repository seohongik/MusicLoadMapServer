package com.project.musicroadmap.external.wikipedia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 위키데이터 → 위키백과 요약 (기획서 14.1, 14.2 ⑤). 위키미디어 API도 User-Agent에 연락처를 요구한다.
 * 소개 글은 부가 정보라 실패하면 예외 대신 빈 값을 돌려준다.
 */
@Slf4j
@Component
public class WikipediaClient {

    private final RestClient restClient;

    public WikipediaClient(RestClient.Builder builder, @Value("${app.musicbrainz.user-agent}") String userAgent) {
        this.restClient = builder.defaultHeader(HttpHeaders.USER_AGENT, userAgent).build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EntityResponse(Map<String, Entity> entities) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entity(Map<String, Sitelink> sitelinks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Sitelink(String title) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SummaryResponse(String type, String extract, @JsonProperty("content_urls") ContentUrls contentUrls) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentUrls(Page desktop) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Page(String page) {
    }

    public record Summary(String extract, String url) {
    }

    /** 위키데이터 항목의 언어별 위키백과 문서 제목 (예: Q11649, "kowiki" → "너바나 (밴드)") */
    public Optional<String> articleTitle(String wikidataId, String lang) {
        try {
            EntityResponse response = restClient.get()
                    .uri("https://www.wikidata.org/wiki/Special:EntityData/{id}.json", wikidataId)
                    .retrieve()
                    .body(EntityResponse.class);
            return Optional.ofNullable(response)
                    .map(EntityResponse::entities)
                    .map(e -> e.get(wikidataId))
                    .map(Entity::sitelinks)
                    .map(links -> links.get(lang + "wiki"))
                    .map(Sitelink::title);
        } catch (RestClientException e) {
            log.warn("위키데이터 조회 실패 {}: {}", wikidataId, e.getMessage());
            return Optional.empty();
        }
    }

    /** 문서 요약. 동음이의 문서이거나 비어 있으면 빈 값 */
    public Optional<Summary> summary(String lang, String title) {
        try {
            SummaryResponse response = restClient.get()
                    .uri("https://{lang}.wikipedia.org/api/rest_v1/page/summary/{title}", lang, title.replace(' ', '_'))
                    .retrieve()
                    .body(SummaryResponse.class);
            if (response == null || "disambiguation".equals(response.type())
                    || response.extract() == null || response.extract().isBlank()) {
                return Optional.empty();
            }
            String url = response.contentUrls() != null && response.contentUrls().desktop() != null
                    ? response.contentUrls().desktop().page()
                    : "https://" + lang + ".wikipedia.org/wiki/" + title.replace(' ', '_');
            return Optional.of(new Summary(response.extract().trim(), url));
        } catch (RestClientException e) {
            log.warn("위키백과 요약 실패 {}:{}: {}", lang, title, e.getMessage());
            return Optional.empty();
        }
    }
}
