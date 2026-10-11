package com.project.musicroadmap.archive.view;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 태그별 묶음 (항목이 많은 태그부터). 태그가 여럿이면 각 태그에 모두 나온다. 태그가 없으면 "태그 없음" */
@Component
public class ByTagView implements ArchiveViewStrategy {

    @Override
    public ArchiveViewType type() {
        return ArchiveViewType.BY_TAG;
    }

    @Override
    public List<ArchiveSection> arrange(List<ArchiveItem> items, GenreGraph graph) {
        Map<String, List<ArchiveItem>> byTag = new LinkedHashMap<>();
        List<ArchiveItem> untagged = new ArrayList<>();
        for (ArchiveItem item : items) {
            if (item.tags().isEmpty()) {
                untagged.add(item);
            }
            item.tags().forEach(tag -> byTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(item));
        }
        List<ArchiveSection> sections = new ArrayList<>(byTag.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, List<ArchiveItem>> e) -> -e.getValue().size())
                        .thenComparing(Map.Entry::getKey))
                .map(e -> new ArchiveSection("#" + e.getKey(),
                        e.getValue().stream().sorted(Comparator.comparing(ArchiveItem::name)).toList()))
                .toList());
        if (!untagged.isEmpty()) {
            sections.add(new ArchiveSection("태그 없음", untagged));
        }
        return sections;
    }
}
