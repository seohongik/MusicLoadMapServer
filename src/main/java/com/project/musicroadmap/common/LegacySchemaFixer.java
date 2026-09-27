package com.project.musicroadmap.common;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * MySQL에 남은 옛 스키마를 정리한다.
 * ddl-auto: update는 컬럼·제약을 추가만 하고 지우거나 줄이지 않아서, 엔티티를 바꿔도 옛 흔적이 남는다.
 * - artist.name 단일 컬럼 유니크 인덱스: 동명이인 가수를 가져올 수 없게 만든다 (지금은 mbid로 구분)
 * - artist.name 길이 100 → 200
 * - roadmap.plan_type: 듣기 방식 제거 후 남은 필수 컬럼. 있으면 로드맵 생성이 실패한다
 * 이미 정리된 DB에서는 아무것도 하지 않는다. MySQL이 아니면(H2 등) 건너뛴다.
 * 나중에 스키마 변경이 잦아지면 Flyway 같은 마이그레이션 도구로 옮긴다.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class LegacySchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySql()) {
            return;
        }
        dropSingleColumnUniqueIndexes("artist", "name");
        widenVarchar("artist", "name", 200);
        dropColumnIfExists("roadmap", "plan_type");
    }

    private boolean isMySql() {
        Boolean mysql = jdbc.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql"));
        return Boolean.TRUE.equals(mysql);
    }

    /** 이 컬럼 하나로만 이루어진 유니크 인덱스를 지운다 (여러 컬럼을 묶은 유니크 제약은 건드리지 않음) */
    private void dropSingleColumnUniqueIndexes(String table, String column) {
        List<String> indexes = jdbc.queryForList("""
                SELECT s.INDEX_NAME
                FROM information_schema.STATISTICS s
                WHERE s.TABLE_SCHEMA = DATABASE() AND s.TABLE_NAME = ? AND s.COLUMN_NAME = ?
                  AND s.NON_UNIQUE = 0 AND s.INDEX_NAME <> 'PRIMARY'
                  AND (SELECT COUNT(*) FROM information_schema.STATISTICS x
                       WHERE x.TABLE_SCHEMA = s.TABLE_SCHEMA AND x.TABLE_NAME = s.TABLE_NAME
                         AND x.INDEX_NAME = s.INDEX_NAME) = 1
                """, String.class, table, column);
        for (String index : indexes) {
            jdbc.execute("ALTER TABLE " + quote(table) + " DROP INDEX " + quote(index));
            log.info("옛 스키마 정리: {}.{} 유니크 인덱스 {} 삭제", table, column, index);
        }
    }

    private void widenVarchar(String table, String column, int length) {
        List<Integer> current = jdbc.queryForList("""
                SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? AND DATA_TYPE = 'varchar'
                """, Integer.class, table, column);
        if (current.isEmpty() || current.get(0) >= length) {
            return;
        }
        jdbc.execute("ALTER TABLE " + quote(table) + " MODIFY " + quote(column) + " VARCHAR(" + length + ") NOT NULL");
        log.info("옛 스키마 정리: {}.{} 길이 {} → {}", table, column, current.get(0), length);
    }

    private void dropColumnIfExists(String table, String column) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        if (count != null && count > 0) {
            jdbc.execute("ALTER TABLE " + quote(table) + " DROP COLUMN " + quote(column));
            log.info("옛 스키마 정리: {}.{} 컬럼 삭제", table, column);
        }
    }

    private static String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
