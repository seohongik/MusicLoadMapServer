package com.project.musicroadmap.archive.view;

import com.project.musicroadmap.genre.GenreGraph;
import java.util.List;

/** 같은 아카이브를 다르게 묶어 보여준다. 새 보기 = enum 1개 + @Component 1개 (기획서 5.0) */
public interface ArchiveViewStrategy {

    ArchiveViewType type();

    List<ArchiveSection> arrange(List<ArchiveItem> items, GenreGraph graph);
}
