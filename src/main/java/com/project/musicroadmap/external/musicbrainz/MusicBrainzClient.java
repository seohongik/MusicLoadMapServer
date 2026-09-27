package com.project.musicroadmap.external.musicbrainz;

import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * MusicBrainz 호출 (기획서 14.2).
 * - 요청은 IP당 초당 1회: 요청 사이에 min-interval-ms 이상 간격을 둔다 (서버 1대 기준 단순 구현)
 * - User-Agent 필수
 * - 503(요청 과다)이면 잠시 뒤 최대 2회 재시도
 */
@Slf4j
@Component
public class MusicBrainzClient {

    private static final int MAX_RETRIES = 2;

    private final RestClient restClient;
    private final long minIntervalMillis;
    private long lastRequestAt = 0;

    public MusicBrainzClient(
            RestClient.Builder builder,
            @Value("${app.musicbrainz.base-url}") String baseUrl,
            @Value("${app.musicbrainz.user-agent}") String userAgent,
            @Value("${app.musicbrainz.min-interval-ms:1100}") long minIntervalMillis) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
        this.minIntervalMillis = minIntervalMillis;
    }

    public List<MbArtist> searchArtists(String keyword, int limit) {
        String query = escapeLucene(keyword.trim());
        MbArtistSearchResponse response = execute(() -> restClient.get()
                .uri(b -> b.path("/artist")
                        .queryParam("query", "{query}")
                        .queryParam("limit", limit)
                        .queryParam("fmt", "json")
                        .build(query))
                .retrieve()
                .body(MbArtistSearchResponse.class));
        return response == null || response.artists() == null ? List.of() : response.artists();
    }

    /** 장르 투표를 포함한 아티스트 상세 */
    public MbArtist getArtist(String mbid) {
        MbArtist artist = execute(() -> restClient.get()
                .uri("/artist/{mbid}?inc=genres&fmt=json", mbid)
                .retrieve()
                .body(MbArtist.class));
        if (artist == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_ARTIST_NOT_FOUND);
        }
        return artist;
    }

    /**
     * 아티스트의 앨범 단위 목록 (type=album). 한 번에 100개씩, 최대 maxPages 페이지.
     * release-group-status=website-default: MusicBrainz 웹사이트 기본 목록과 같게 해적판·비공식 음반을 뺀다.
     * 라이브·컴필레이션은 여전히 섞여 오므로 정규 앨범은 호출하는 쪽에서 거른다.
     */
    public List<MbReleaseGroup> browseAlbumGroups(String artistMbid, int maxPages) {
        List<MbReleaseGroup> all = new ArrayList<>();
        for (int page = 0; page < maxPages; page++) {
            int offset = page * 100;
            MbReleaseGroupBrowseResponse response = execute(() -> restClient.get()
                    .uri("/release-group?artist={mbid}&type=album&release-group-status=website-default"
                            + "&limit=100&offset={offset}&fmt=json", artistMbid, offset)
                    .retrieve()
                    .body(MbReleaseGroupBrowseResponse.class));
            if (response == null || response.releaseGroups() == null) {
                break;
            }
            all.addAll(response.releaseGroups());
            if (all.size() >= response.count()) {
                break;
            }
        }
        return all;
    }

    /** 앨범의 발매판 목록 (officialOnly면 공식 발매만) */
    public List<MbRelease> browseReleases(String releaseGroupMbid, boolean officialOnly) {
        String status = officialOnly ? "&status=official" : "";
        MbReleaseBrowseResponse response = execute(() -> restClient.get()
                .uri("/release?release-group={mbid}" + status + "&limit=100&fmt=json", releaseGroupMbid)
                .retrieve()
                .body(MbReleaseBrowseResponse.class));
        return response == null || response.releases() == null ? List.of() : response.releases();
    }

    /** 발매판 하나의 수록곡 */
    public MbRelease getReleaseWithRecordings(String releaseMbid) {
        MbRelease release = execute(() -> restClient.get()
                .uri("/release/{mbid}?inc=recordings&fmt=json", releaseMbid)
                .retrieve()
                .body(MbRelease.class));
        if (release == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_ARTIST_NOT_FOUND);
        }
        return release;
    }

    private <T> T execute(Supplier<T> call) {
        for (int attempt = 0; ; attempt++) {
            throttle();
            try {
                return call.get();
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                        || e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
                    throw new BusinessException(ErrorCode.EXTERNAL_ARTIST_NOT_FOUND);
                }
                if (e.getStatusCode().value() != HttpStatus.SERVICE_UNAVAILABLE.value() || attempt >= MAX_RETRIES) {
                    log.warn("MusicBrainz 호출 실패: {}", e.getStatusCode());
                    throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
                }
                log.info("MusicBrainz 503, 재시도 {}/{}", attempt + 1, MAX_RETRIES);
            } catch (RestClientException e) {
                log.warn("MusicBrainz 연결 실패: {}", e.getMessage());
                throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
            }
        }
    }

    private synchronized void throttle() {
        long wait = minIntervalMillis - (System.currentTimeMillis() - lastRequestAt);
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }

    /** 검색어의 특수문자(예: AC/DC의 /)가 검색 문법으로 해석되지 않도록 이스케이프 */
    static String escapeLucene(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if ("+-&|!(){}[]^\"~*?:\\/".indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
