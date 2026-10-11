package com.project.musicroadmap.relation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 노드 사이의 관계. MEMBER는 사람 → 밴드, COLLABORATION은 작은 ID → 큰 ID로 저장한다 */
@Entity
@Table(name = "artist_relation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_node_id", "to_node_id", "type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromNodeId;

    @Column(nullable = false)
    private Long toNodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelationType type;

    public static ArtistRelation of(Long fromNodeId, Long toNodeId, RelationType type) {
        ArtistRelation relation = new ArtistRelation();
        if (type == RelationType.COLLABORATION && fromNodeId > toNodeId) {
            relation.fromNodeId = toNodeId;
            relation.toNodeId = fromNodeId;
        } else {
            relation.fromNodeId = fromNodeId;
            relation.toNodeId = toNodeId;
        }
        relation.type = type;
        return relation;
    }
}
