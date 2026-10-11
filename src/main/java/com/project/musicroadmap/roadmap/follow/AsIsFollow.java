package com.project.musicroadmap.roadmap.follow;

import com.project.musicroadmap.weight.UserWeights;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AsIsFollow implements FollowStrategy {

    @Override
    public FollowMode mode() {
        return FollowMode.AS_IS;
    }

    @Override
    public List<Long> startPath(List<Long> sharedPath, UserWeights myWeights) {
        return List.copyOf(sharedPath);
    }

    @Override
    public boolean recalculatesOnVerdict() {
        return false;
    }
}
