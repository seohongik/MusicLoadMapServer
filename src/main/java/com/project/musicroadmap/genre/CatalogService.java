package com.project.musicroadmap.genre;

import com.project.musicroadmap.genre.CatalogDtos.ArtistDto;
import com.project.musicroadmap.genre.CatalogDtos.CatalogResponse;
import com.project.musicroadmap.genre.CatalogDtos.EdgeDto;
import com.project.musicroadmap.genre.CatalogDtos.GenreArtistDto;
import com.project.musicroadmap.genre.CatalogDtos.GenreDto;
import com.project.musicroadmap.genre.CatalogDtos.TrackDto;
import com.project.musicroadmap.playback.PlaybackResolver;
import com.project.musicroadmap.playback.PlaybackStrategy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final GenreRepository genreRepository;
    private final GenreEdgeRepository edgeRepository;
    private final TrackRepository trackRepository;
    private final ArtistRepository artistRepository;
    private final GenreArtistRepository genreArtistRepository;
    private final PlaybackResolver playbackResolver;

    public CatalogResponse getCatalog() {
        PlaybackStrategy playback = playbackResolver.current();
        return new CatalogResponse(
                playback.type(),
                genreRepository.findAllByOrderByIdAsc().stream().map(GenreDto::from).toList(),
                edgeRepository.findAll().stream().map(EdgeDto::from).toList(),
                trackRepository.findAllByOrderByIdAsc().stream().map(t -> TrackDto.from(t, playback)).toList(),
                artistRepository.findAllByOrderByNameAsc().stream().map(ArtistDto::from).toList(),
                genreArtistRepository.findAllByOrderByGenreIdAscDisplayOrderAsc().stream().map(GenreArtistDto::from).toList());
    }

    public List<ArtistDto> searchArtists(String keyword) {
        return artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim()).stream()
                .map(ArtistDto::from)
                .toList();
    }
}
