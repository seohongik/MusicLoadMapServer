package com.project.musicroadmap.personal;

import com.project.musicroadmap.auth.UserService;
import com.project.musicroadmap.common.AppClock;
import com.project.musicroadmap.common.BusinessException;
import com.project.musicroadmap.common.ErrorCode;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.genre.GenreGraphProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 나만의 연결과 유저별 그래프 (기획서 15.2 규칙 5, 15.4).
 * 로드맵 계산은 기본적으로 참조 그래프로 하고, 유저가 켜면 내 연결을 덧씌운 그래프로 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserGraphService {

    private final UserEdgeRepository edgeRepository;
    private final GenreGraphProvider graphProvider;
    private final UserService userService;
    private final AppClock clock;

    public record UserEdgeRequest(@NotNull Long fromGenreId, @NotNull Long toGenreId, @Size(max = 200) String note) {
    }

    public record UserEdgeResponse(Long fromGenreId, Long toGenreId, String note, LocalDateTime createdAt) {
        static UserEdgeResponse from(UserEdge e) {
            return new UserEdgeResponse(e.getFromGenreId(), e.getToGenreId(), e.getNote(), e.getCreatedAt());
        }
    }

    /** 로드맵 계산에 쓸 그래프. 내 연결을 켰을 때만 덧씌운다 */
    @Transactional(readOnly = true)
    public GenreGraph graphFor(Long userId) {
        GenreGraph base = graphProvider.get();
        if (!userService.usesMyEdges(userId)) {
            return base;
        }
        return base.withExtraEdges(edgeRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(e -> new GenreGraph.Edge(e.getFromGenreId(), e.getToGenreId(), UserEdge.BASE_COST))
                .toList());
    }

    @Transactional(readOnly = true)
    public List<UserEdgeResponse> edges(Long userId) {
        return edgeRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(UserEdgeResponse::from).toList();
    }

    /**
     * 계보 의미를 지키는 규칙: 같은 장르 X, 이미 계보도에 있는 연결 X,
     * 계보도 + 내 연결 전체에서 순환이 생기면 X (시대순 여행·후손 찾기가 제자리로 돌아오지 않도록)
     */
    public UserEdgeResponse add(Long userId, UserEdgeRequest request) {
        long from = request.fromGenreId();
        long to = request.toGenreId();
        GenreGraph base = graphProvider.get();
        base.node(from);
        base.node(to);
        if (from == to) {
            throw new BusinessException(ErrorCode.USER_EDGE_SAME_GENRE);
        }
        if (base.connected(from, to)) {
            throw new BusinessException(ErrorCode.USER_EDGE_EXISTS);
        }
        List<UserEdge> mine = edgeRepository.findByUserIdOrderByCreatedAtAsc(userId);
        if (mine.stream().anyMatch(e -> e.getFromGenreId() == from && e.getToGenreId() == to
                || e.getFromGenreId() == to && e.getToGenreId() == from)) {
            throw new BusinessException(ErrorCode.USER_EDGE_EXISTS);
        }
        GenreGraph withMine = base.withExtraEdges(mine.stream()
                .map(e -> new GenreGraph.Edge(e.getFromGenreId(), e.getToGenreId(), UserEdge.BASE_COST))
                .toList());
        if (withMine.wouldCreateCycle(from, to)) {
            throw new BusinessException(ErrorCode.USER_EDGE_CYCLE);
        }
        String note = request.note() == null ? "" : request.note().trim();
        return UserEdgeResponse.from(edgeRepository.save(UserEdge.of(userId, from, to, note, clock.now())));
    }

    public void delete(Long userId, Long fromGenreId, Long toGenreId) {
        edgeRepository.findByUserIdAndFromGenreIdAndToGenreId(userId, fromGenreId, toGenreId)
                .ifPresent(edgeRepository::delete);
    }
}
