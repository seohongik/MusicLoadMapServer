package com.project.musicroadmap.external.musicbrainz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MusicBrainzClientTest {

    @Test
    void 검색어의_특수문자는_이스케이프한다() {
        assertThat(MusicBrainzClient.escapeLucene("AC/DC")).isEqualTo("AC\\/DC");
        assertThat(MusicBrainzClient.escapeLucene("Guns N' Roses")).isEqualTo("Guns N' Roses");
        assertThat(MusicBrainzClient.escapeLucene("!!!")).isEqualTo("\\!\\!\\!");
    }
}
