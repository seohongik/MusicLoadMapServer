package com.project.musicroadmap.archive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 앨범 수록곡 (참조 레이어). 곡별 좋아요/별로의 대상이 된다 (TargetType.ALBUM_TRACK) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlbumTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id")
    private Album album;

    /** CD 여러 장짜리 앨범의 몇 번째 디스크인지 */
    @Column(nullable = false)
    private int discNumber;

    /** 디스크 안에서의 곡 번호 (정렬용) */
    @Column(nullable = false)
    private int trackNumber;

    @Column(nullable = false, length = 500)
    private String title;

    private Integer lengthMs;

    @Column(length = 36)
    private String recordingMbid;

    public static AlbumTrack of(Album album, int discNumber, int trackNumber, String title, Integer lengthMs, String recordingMbid) {
        AlbumTrack track = new AlbumTrack();
        track.album = album;
        track.discNumber = discNumber;
        track.trackNumber = trackNumber;
        track.title = title;
        track.lengthMs = lengthMs;
        track.recordingMbid = recordingMbid;
        return track;
    }
}
