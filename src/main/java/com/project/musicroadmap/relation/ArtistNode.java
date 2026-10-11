package com.project.musicroadmap.relation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관계 그래프의 노드 (참조 레이어, 기획서 24장). 우리 DB의 가수(artistId)뿐 아니라
 * 관계에 등장한 멤버·협업자도 이름과 MusicBrainz ID만 담아 둔다 (선택하면 그때 가져온다).
 */
@Entity
@Table(name = "artist_node")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String mbid;

    @Column(nullable = false, length = 200)
    private String name;

    /** Person / Group 등 (MusicBrainz 값) */
    @Column(length = 30)
    private String type;

    /** 우리 DB에 있는 가수면 그 ID */
    private Long artistId;

    /** 이 노드의 관계를 마지막으로 가져온 시각 (null이면 아직 안 가져옴) */
    private LocalDateTime relationsSyncedAt;

    public static ArtistNode of(String mbid, String name, String type, Long artistId) {
        ArtistNode node = new ArtistNode();
        node.mbid = mbid;
        node.name = name.length() > 200 ? name.substring(0, 200) : name;
        node.type = type;
        node.artistId = artistId;
        return node;
    }

    public void linkArtist(Long artistId) {
        if (this.artistId == null) {
            this.artistId = artistId;
        }
    }

    public void assignTypeIfMissing(String type) {
        if (this.type == null) {
            this.type = type;
        }
    }

    public void markSynced(LocalDateTime now) {
        this.relationsSyncedAt = now;
    }

    public boolean isGroup() {
        return "Group".equals(type);
    }
}
