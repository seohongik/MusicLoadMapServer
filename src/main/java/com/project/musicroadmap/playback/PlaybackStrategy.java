package com.project.musicroadmap.playback;

import com.project.musicroadmap.genre.Track;
import java.util.Optional;

/** 곡마다 "듣기" 링크를 줄지, 준다면 어디로 보낼지 정한다. 앱은 링크가 있을 때만 듣기 버튼을 보여준다 */
public interface PlaybackStrategy {

    PlaybackType type();

    Optional<String> listenUrl(Track track);
}
