package com.project.musicroadmap.archive.view;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/** 10년 단위 묶음, 오래된 순. 연도를 모르는 항목은 맨 뒤 "연도 모름" */
@Component
public class TimelineView implements ArchiveViewStrategy {

    @Override
    public ArchiveViewType type() {
        return ArchiveViewType.TIMELINE;
    }

    @Override
    public List<ArchiveSection> arrange(List<ArchiveItem> items, GenreGraph graph) {
        Map<Integer, List<ArchiveItem>> byDecade = new TreeMap<>();
        List<ArchiveItem> unknown = new ArrayList<>();
        for (ArchiveItem item : items) {
            if (item.year() == null) {
                unknown.add(item);
            } else {
                byDecade.computeIfAbsent(item.year() / 10 * 10, k -> new ArrayList<>()).add(item);
            }
        }
        List<ArchiveSection> sections = new ArrayList<>();
        byDecade.forEach((decade, list) -> sections.add(new ArchiveSection(decade + "년대",
                list.stream().sorted(Comparator.comparing(ArchiveItem::year).thenComparing(ArchiveItem::name)).toList())));
        if (!unknown.isEmpty()) {
            sections.add(new ArchiveSection("연도 모름", unknown.stream().sorted(Comparator.comparing(ArchiveItem::name)).toList()));
        }
        return sections;
    }
}
