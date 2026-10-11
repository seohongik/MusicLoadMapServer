package com.project.musicroadmap.archive.view;

import com.project.musicroadmap.archive.Album;
import com.project.musicroadmap.archive.AlbumRepository;
import com.project.musicroadmap.genre.Artist;
import com.project.musicroadmap.genre.ArtistRepository;
import com.project.musicroadmap.genre.GenreGraph;
import com.project.musicroadmap.genre.GenreGraphProvider;
import com.project.musicroadmap.genre.Track;
import com.project.musicroadmap.genre.TrackRepository;
import com.project.musicroadmap.personal.PersonalTargetType;
import com.project.musicroadmap.personal.Review;
import com.project.musicroadmap.personal.ReviewRepository;
import com.project.musicroadmap.personal.UserGenreOverride;
import com.project.musicroadmap.personal.UserGenreOverrideRepository;
import com.project.musicroadmap.personal.UserTag;
import com.project.musicroadmap.personal.UserTagRepository;
import com.project.musicroadmap.preference.Preference;
import com.project.musicroadmap.preference.TargetType;
import com.project.musicroadmap.preference.UserPreference;
import com.project.musicroadmap.preference.UserPreferenceRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기획서 23장: 내 아카이브 = 내가 감상·태그·좋아요를 남긴 장르, 가수, 앨범.
 * 항목은 한 번 모으고, 묶는 방식은 보기 전략이 정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyArchiveService {

    private final ReviewRepository reviewRepository;
    private final UserTagRepository tagRepository;
    private final UserGenreOverrideRepository overrideRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final TrackRepository trackRepository;
    private final GenreGraphProvider graphProvider;
    private final ArchiveViewResolver viewResolver;

    public record ArchiveViewResponse(ArchiveViewType view, List<ArchiveViewType> views, int itemCount,
                                      List<ArchiveSection> sections) {
    }

    public ArchiveViewResponse view(Long userId, ArchiveViewType type) {
        GenreGraph graph = graphProvider.get();
        List<ArchiveItem> items = collect(userId, graph);
        return new ArchiveViewResponse(type, viewResolver.available(), items.size(),
                viewResolver.get(type).arrange(items, graph));
    }

    private record Key(PersonalTargetType type, Long id) {
    }

    List<ArchiveItem> collect(Long userId, GenreGraph graph) {
        Map<Key, Review> reviews = reviewRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .collect(Collectors.toMap(r -> new Key(r.getTargetType(), r.getTargetId()), r -> r));
        Map<Key, List<String>> tags = tagRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .collect(Collectors.groupingBy(t -> new Key(t.getTargetType(), t.getTargetId()),
                        Collectors.mapping(UserTag::getName, Collectors.toList())));
        Set<Long> likedArtists = preferenceRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .filter(p -> p.getTargetType() == TargetType.ARTIST && p.getPreference() == Preference.LIKE)
                .map(UserPreference::getTargetId)
                .collect(Collectors.toSet());
        Map<Long, Long> myGenres = overrideRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .collect(Collectors.toMap(UserGenreOverride::getArtistId, UserGenreOverride::getGenreId));

        Set<Key> keys = new LinkedHashSet<>();
        keys.addAll(reviews.keySet());
        keys.addAll(tags.keySet());
        likedArtists.forEach(id -> keys.add(new Key(PersonalTargetType.ARTIST, id)));

        List<ArchiveItem> items = new ArrayList<>();
        for (Key key : keys) {
            Review review = reviews.get(key);
            Integer rating = review == null ? null : review.getRating();
            List<String> itemTags = tags.getOrDefault(key, List.of());
            item(key, graph, myGenres, rating, review != null, likedArtists, itemTags).ifPresent(items::add);
        }
        return items;
    }

    private Optional<ArchiveItem> item(Key key, GenreGraph graph, Map<Long, Long> myGenres, Integer rating,
                                       boolean reviewed, Set<Long> likedArtists, List<String> tags) {
        return switch (key.type()) {
            case GENRE -> graph.contains(key.id())
                    ? Optional.of(new ArchiveItem(key.type(), key.id(), graph.node(key.id()).nameKo(),
                    graph.node(key.id()).originDecade(), key.id(), rating, false, reviewed, tags))
                    : Optional.empty();
            case ARTIST -> artistRepository.findById(key.id()).map(artist -> new ArchiveItem(key.type(), artist.getId(),
                    artist.getName(), artistYear(artist), genreOf(artist, myGenres), rating,
                    likedArtists.contains(artist.getId()), reviewed, tags));
            case ALBUM -> albumRepository.findWithArtistById(key.id()).map(album -> new ArchiveItem(key.type(), album.getId(),
                    album.getTitle() + " · " + album.getArtist().getName(), album.getReleaseYear(),
                    genreOf(album.getArtist(), myGenres), rating, false, reviewed, tags));
        };
    }

    /** 내 분류가 있으면 내 분류 (15.2 규칙 4) */
    private static Long genreOf(Artist artist, Map<Long, Long> myGenres) {
        Long mine = myGenres.get(artist.getId());
        if (mine != null) {
            return mine;
        }
        return artist.getPrimaryGenre() == null ? null : artist.getPrimaryGenre().getId();
    }

    /** 가장 이른 대표곡·앨범 연도 */
    private Integer artistYear(Artist artist) {
        return Stream.concat(
                        trackRepository.findByArtistId(artist.getId()).stream().map(Track::getReleaseYear),
                        albumRepository.findByArtistIdOrderByReleaseYearAscIdAsc(artist.getId()).stream()
                                .map(Album::getReleaseYear).filter(Objects::nonNull))
                .min(Integer::compare)
                .orElse(null);
    }
}
