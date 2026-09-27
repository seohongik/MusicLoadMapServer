package com.project.musicroadmap.genre.seed;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import tools.jackson.databind.json.JsonMapper;

/** 큐레이션 데이터 파일을 읽는다. 데이터 자체는 seed-data 폴더의 JSON에 있다 (기획서 20장) */
public final class SeedData {

    public static final String MAIN = "seed/catalog.json";
    /** 테스트 전용 고정 데이터 (록 계보 28장르) */
    public static final String FIXTURE = "seed/test-catalog.json";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private SeedData() {
    }

    public static SeedCatalog load(String classpathLocation) {
        try (InputStream in = SeedData.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            if (in == null) {
                throw new IllegalStateException("큐레이션 데이터 파일이 없습니다: " + classpathLocation);
            }
            return JSON.readValue(in, SeedCatalog.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SeedCatalog fixture() {
        return load(FIXTURE);
    }
}
