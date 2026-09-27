# MusicRoadmapServer

개인화 음악 장르 계보 로드맵 서버 (Spring Boot 4 / Java 17 / MySQL).
유저마다 다른 결과를 내는 로직을 전략 패턴으로 구현한다.

## 실행
```bash
# 환경변수: DB_USERNAME, DB_PASSWORD, MUSICBRAINZ_CONTACT
./gradlew bootRun --args='--spring.profiles.active=local'   # MySQL
./gradlew bootRun --args='--spring.profiles.active=h2'      # 메모리 DB
./gradlew test
```

## 기획서
[docs/music_roadmap_v3.md](docs/music_roadmap_v3.md) — 기획·전략 설계·데이터 구조·개발 단계

## 큐레이션 데이터
`seed-data/main/seed/catalog.json` — 장르·간선·대표곡·별칭. 서버가 켜질 때 없는 것만 DB에 추가한다.
안드로이드 앱([MusicLoadMapAndroid](https://github.com/seohongik/MusicLoadMapAndroid))도 이 파일을 함께 쓴다.

## 브랜치
- `main`: 안정 버전
- `develop`: 작업 중인 버전
- `feature/*`: develop에서 따서 작업 후 develop에 합침
