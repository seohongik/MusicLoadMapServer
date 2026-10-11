package com.project.musicroadmap.genre.intro;

import java.util.Optional;

/** 출처가 늘어도 서비스 코드는 바뀌지 않는다. 새 출처 = enum 1개 + @Component 1개 */
public interface ArtistIntroSource {

    IntroSourceType type();

    /** MusicBrainz ID로 소개 글을 찾는다. 없으면 빈 값 */
    Optional<ArtistIntro> fetch(String mbid);
}
