package com.project.musicroadmap.relation;

import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.ArtistRepository;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 관계망 채우기 (기획서 24장): 1단계 우리 DB 가수 → 2단계 그 관계에 나온 사람(멤버)의 관계.
 * 사람을 거쳐야 밴드끼리 이어지므로 2단계까지 가져온다. 이미 가져온 노드는 건너뛴다 (다시 실행해도 안전).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelationBatch {

    private final ArtistRepository artistRepository;
    private final ArtistNodeRepository nodeRepository;
    private final RelationSyncService syncService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 이미 돌고 있으면 false */
    public boolean startIfIdle() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        Thread thread = new Thread(() -> {
            try {
                run();
            } finally {
                running.set(false);
            }
        }, "relation-batch");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    void run() {
        int synced = 0;
        for (Artist artist : artistRepository.findAllByOrderByNameAsc()) {
            ArtistNode node = syncService.nodeOf(artist).orElse(null);
            if (node != null && syncService.needsSync(node) && syncService.sync(node)) {
                synced++;
            }
        }
        log.info("관계망 1단계 끝: 가수 {}명", synced);
        int people = 0;
        for (ArtistNode person : nodeRepository.findByRelationsSyncedAtIsNullAndTypeOrderByIdAsc("Person")) {
            if (syncService.sync(person)) {
                people++;
            }
        }
        log.info("관계망 2단계 끝: 사람 {}명", people);
    }
}
