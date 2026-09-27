package com.project.musicroadmap.genre;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByName(String name);

    Optional<Artist> findFirstByNameIgnoreCaseAndSourceOrderByIdAsc(String name, DataSource source);

    @EntityGraph(attributePaths = "primaryGenre")
    Optional<Artist> findByMbid(String mbid);

    List<Artist> findByMbidIn(List<String> mbids);

    /** 외부 ID가 아직 없는 같은 이름의 아티스트 (큐레이션 데이터와 연결할 때) */
    Optional<Artist> findFirstByNameIgnoreCaseAndMbidIsNull(String name);

    @EntityGraph(attributePaths = "primaryGenre")
    List<Artist> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword);

    @EntityGraph(attributePaths = "primaryGenre")
    List<Artist> findAllByOrderByNameAsc();
}
