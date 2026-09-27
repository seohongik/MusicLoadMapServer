package com.project.musicroadmap.genre;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** DB의 계보를 한 번 읽어 메모리 그래프로 들고 있는다. 시드 데이터가 바뀌면 reload() */
@Component
@RequiredArgsConstructor
public class GenreGraphProvider {

    private final GenreRepository genreRepository;
    private final GenreEdgeRepository edgeRepository;

    private volatile GenreGraph graph;

    @Transactional(readOnly = true)
    public GenreGraph get() {
        GenreGraph current = graph;
        if (current == null) {
            synchronized (this) {
                if (graph == null) {
                    graph = load();
                }
                current = graph;
            }
        }
        return current;
    }

    @Transactional(readOnly = true)
    public synchronized void reload() {
        graph = load();
    }

    private GenreGraph load() {
        List<GenreGraph.Node> nodes = genreRepository.findAllByOrderByIdAsc().stream()
                .map(g -> new GenreGraph.Node(g.getId(), g.getNameKo(), g.getOriginDecade()))
                .toList();
        List<GenreGraph.Edge> edges = edgeRepository.findAll().stream()
                .map(e -> new GenreGraph.Edge(e.getFromGenre().getId(), e.getToGenre().getId(), e.getBaseCost()))
                .toList();
        return new GenreGraph(nodes, edges);
    }
}
