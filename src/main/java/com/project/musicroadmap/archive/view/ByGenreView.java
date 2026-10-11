package com.project.musicroadmap.archive.view;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/** 장르별 묶음, 장르는 등장 시기 순. 장르를 모르는 항목은 "분류 없음" */
@Component
public class ByGenreView implements ArchiveViewStrategy {

    @Override
    public ArchiveViewType type() {
        return ArchiveViewType.BY_GENRE;
    }

    @Override
    public List<ArchiveSection> arrange(List<ArchiveItem> items, GenreGraph graph) {
        Map<Long, List<ArchiveItem>> byGenre = new TreeMap<>(Comparator
                .comparingInt((Long id) -> graph.node(id).originDecade())
                .thenComparing(id -> id));
        List<ArchiveItem> unknown = new ArrayList<>();
        for (ArchiveItem item : items) {
            if (item.genreId() == null || !graph.contains(item.genreId())) {
                unknown.add(item);
            } else {
                byGenre.computeIfAbsent(item.genreId(), k -> new ArrayList<>()).add(item);
            }
        }
        List<ArchiveSection> sections = new ArrayList<>();
        byGenre.forEach((genreId, list) -> sections.add(new ArchiveSection(graph.node(genreId).nameKo(),
                list.stream().sorted(Comparator.comparing(ArchiveItem::targetType).thenComparing(ArchiveItem::name)).toList())));
        if (!unknown.isEmpty()) {
            sections.add(new ArchiveSection("분류 없음", unknown));
        }
        return sections;
    }
}
