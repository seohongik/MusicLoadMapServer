package com.project.musicroadmap.roadmap.follow;

import com.project.musicroadmap.weight.UserWeights;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdaptFollow implements FollowStrategy {

    @Override
    public FollowMode mode() {
        return FollowMode.ADAPT;
    }

    @Override
    public List<Long> startPath(List<Long> sharedPath, UserWeights myWeights) {
        List<Long> path = new ArrayList<>();
        path.add(sharedPath.get(0));
        sharedPath.stream().skip(1).filter(id -> !myWeights.isDisliked(id)).forEach(path::add);
        return path;
    }

    @Override
    public boolean recalculatesOnVerdict() {
        return true;
    }
}
