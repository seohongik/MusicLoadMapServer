# 음악 계보 로드맵 기획서 v3

> v1·v2(인생 성적표)에서 아이템을 바꿨다.
> 이 프로젝트의 기술 목표는 **"유저마다 다른 로직을 런타임에 골라 끼우는 백엔드(전략 패턴)"** 를 만드는 것이다.
> `[Phase N]` 표시는 구현 단계를 뜻한다. (Phase 1 = MVP)

---

## 0. 한 줄 소개

**좋아하는 음악에서 출발해 장르 계보를 따라 나만의 음악 탐험 경로를 만들어 주는 앱**

### Why
- 좋아하는 음악은 있는데, **거기서 어디로 넓혀가야 할지 모른다.**
- 스트리밍 앱 추천은 **비슷한 곡**만 계속 준다. 취향이 넓어지지 않는다.
- 이 앱은 비슷함이 아니라 **계보(어디서 왔고 어디로 갔는지)** 를 따라 이동한다. "오아시스를 좋아한다면 비틀즈를 거쳐 블루스까지"처럼 음악의 맥락을 경험하게 한다.

### 타깃 유저
- 최애 밴드나 장르가 한두 개로 굳어진 20~30대 리스너
- "음악 좀 안다"는 친구의 추천이 부러운 사람

### 핵심 경험
1. 좋아하는 장르나 아티스트를 고른다.
2. 탐험 방식을 고른다: 뿌리 찾기 / 후손 찾기 / 두 장르 잇기
3. 장르별 대표곡을 듣고 좋아요·별로·애매함과 한 줄 감상을 남긴다.
4. 반응에 따라 **남은 경로가 다시 계산**된다.
5. 지나온 장르가 계보도 위에 표시되어 **"나만의 음악 계보도"** 가 쌓인다.

---

## 1. 기술 스택

| 구분 | 선택 |
|---|---|
| 프론트 | Android 네이티브 (Kotlin + Jetpack Compose) |
| 백엔드 | Spring Boot 4.x, Java 17, Spring Data JPA, MySQL |
| 인증 | 구글 로그인 → 서버에서 ID 토큰 검증 → 자체 JWT 발급 |
| 앱 ↔ 서버 | Retrofit + Gson. 에뮬레이터에서는 `http://10.0.2.2:8080/`. `USE_FAKE_DATA=true`면 서버 없이 앱 안의 가짜 저장소로 동작 |
| 서버 DB 없이 실행 | `./gradlew bootRun --args='--spring.profiles.active=h2'` (메모리 DB, 끄면 데이터 사라짐) |
| 그래프 처리 | 장르 그래프를 서버 시작 시 **메모리에 적재**하여 탐색. 그래프 DB는 사용하지 않음 |
| 음악 재생 | **소개 중심, 기본은 재생 없음.** 재생 방식은 `PlaybackStrategy`로 바꿀 수 있다 (17장) |
| 시간대 | Asia/Seoul |

---

## 2. 핵심 개념

### 2.1 장르 그래프
- **노드 = 장르**, **간선 = 영향 관계** (기원 장르 → 파생 장르, 방향 있음)
  - 예: `블루스 → R&B` = "R&B는 블루스에서 나왔다"
- 모든 간선의 기본 비용 `baseCost`는 **1.0**이다. (Phase 2에서 영향 강도에 따라 차등 적용 검토)
- 그래프 데이터는 **관리자가 큐레이션한 한 가지 관점**이다. 앱 안에 "장르 분류는 관점에 따라 다를 수 있음"을 안내한다.

### 2.2 개인 가중치 (이 앱의 핵심)
- 유저마다 **장르별 가중치 `multiplier`** 를 가진다. 기본값은 1.0이다.
- **장르 g로 이동하는 비용** = `baseCost × multiplier(유저, g)`
- 같은 그래프, 같은 알고리즘이라도 **유저마다 가중치가 달라서 다른 경로**가 나온다.

| 이벤트 | 가중치 변화 |
|---|---|
| 장르 g를 **싫어함** | `multiplier(g)` × 3.0, 그리고 g의 **파생 장르(자식)** 각각 `multiplier` × 1.5 |
| 장르 g를 **좋아함** | g의 **이웃 장르**(들어오고 나가는 간선 양쪽) 각각 `multiplier` × 0.8 |
| 범위 제한 | 0.5 ≤ multiplier ≤ 10.0 |
| 제외 기준 | multiplier ≥ 3.0인 장르는 **싫어하는 장르**로 보고 경로 후보에서 제외 (단, 두 장르 잇기에서 다른 길이 없으면 통과 허용) |

### 2.3 로드맵
- 로드맵 = **순서가 있는 장르 목록(스텝)** + 스텝마다 들을 곡 목록
- 유저당 **진행 중인 로드맵은 1개**만 허용한다.
- 로드맵은 반응에 따라 **현재 위치 이후의 스텝이 다시 계산**된다. 지나온 스텝은 바뀌지 않는다.

---

## 3. 사용 흐름

```
[로그인]
   ↓
[출발점 정하기] ── 장르 직접 선택 / 좋아하는 아티스트 선택
   ↓
[탐험 방식 선택] ── 뿌리 찾기 / 후손 찾기 / 두 장르 잇기(목표 장르 선택)
   ↓        └─ "미리보기": 세 방식의 경로를 나란히 비교
   ↓
[로드맵 진행] ⇄ [곡 듣기 → 반응 + 한 줄 감상]
   ↓                  └─ 장르 판정 → 가중치 변경 → 남은 경로 재계산
[로드맵 완료]
   ↓
[나의 계보도] ── 지나온 장르와 감상 모아 보기 → 새 로드맵 시작
```

---

## 4. 화면 구성 (Android)

| 화면 | 주요 요소 |
|---|---|
| 온보딩 / 로그인 | 앱 소개 3장, 구글 로그인 |
| 출발점 선택 | 장르 목록 검색, 아티스트 검색 |
| 탐험 방식 선택 | 전략 3개 카드, **미리보기**(전략별 경로 비교), 두 장르 잇기일 때 목표 장르 선택 |
| 홈 (오늘의 음악) | 오늘 들을 장르와 곡, 곡마다 유튜브 링크, 반응 버튼 3개, 한 줄 감상 입력 |
| 로드맵 | 스텝 타임라인 (완료 / 진행 중 / 예정), 경로가 바뀐 경우 "경로가 바뀌었어요" 표시 |
| 나의 계보도 | 전체 계보도 위에 지나온 장르 강조, 장르를 누르면 내 감상 목록 |
| 장르 상세 | 설명, 시대, 기원·파생 장르, 대표곡 |

- 계보도는 장르마다 **고정 좌표(posX, posY)** 를 데이터로 저장해 두고 그 위치에 그린다. 앱에서 그래프 자동 배치를 하지 않아도 된다.

---

## 5. 전략 설계

### 5.0 공통 구조
- 전략마다 **인터페이스 1개 + 구현체 여러 개 + Resolver 1개** 구조를 쓴다.
- DB에는 **enum 값**을 저장하고, Resolver가 enum으로 구현체를 찾는다.
- 새 전략을 추가할 때는 **enum 값 1개 + `@Component` 클래스 1개**만 늘린다. 기존 코드는 수정하지 않는다(OCP).
- **전략 선택에 리플렉션을 직접 쓰지 않는다.** DB에 클래스명·메서드명을 저장하고 `Class.forName`/`Method.invoke`로 호출하는 방식은 금지한다.
  - 이유: 이름을 바꾸면 컴파일 때 못 잡고 실행 중에 터진다 / DB 값이 조작되면 의도하지 않은 메서드가 호출될 수 있다 / enum + Resolver로 같은 일을 타입 안전하게 할 수 있다.
  - 구현체 목록 수집은 스프링 DI(`List<ExplorationStrategy>` 주입)에 맡긴다. 스프링 내부의 리플렉션은 **시작 시 1회**만 일어나므로 요청마다 비용이 들지 않는다.

```java
public interface ExplorationStrategy {
    ExplorationType type();
    List<Long> explore(GenreGraph graph, UserWeights weights, ExplorationRequest request);
}

@Component
public class ExplorationStrategyResolver {
    private final Map<ExplorationType, ExplorationStrategy> strategies;

    public ExplorationStrategyResolver(List<ExplorationStrategy> list) {
        this.strategies = list.stream()
                .collect(Collectors.toMap(ExplorationStrategy::type, Function.identity()));
    }

    public ExplorationStrategy get(ExplorationType type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_EXPLORATION));
    }
}
```

### 5.1 출발점 판정 `StartPointResolver` `[Phase 1]`

| 구현체 | 입력 | 출력 |
|---|---|---|
| `GENRE` 장르 직접 선택 | genreId | 해당 장르 |
| `ARTIST` 아티스트 선택 | artistId | 아티스트의 **대표 장르** (큐레이션 데이터) |
| `MULTI_ARTIST` 여러 아티스트 `[Phase 2]` | artistId 3~5개 | 대표 장르 중 **가장 많이 나온 장르**. 동률이면 그 장르들 사이의 중간 지점 |

### 5.2 탐험 방식 `ExplorationStrategy` `[Phase 1]`

공통 규칙
- 출력: 출발 장르를 **포함한** 장르 ID 목록 (출발 장르가 스텝 0)
- 이미 경로에 들어간 장르는 다시 방문하지 않는다. (순환 방지)
- 후보가 여러 개면 **이동 비용이 가장 낮은 장르**를 고른다.
- 동률 처리: ① 비용 낮은 순 → ② 등장 시기(originDecade) 빠른 순 → ③ 장르 ID 작은 순
- `BRIDGE`의 비용이 같은 경로가 여러 개면: 우선순위 큐를 (누적 비용, 장르 ID) 순으로 꺼내고 비용이 **더 작을 때만** 갱신한다. 즉 먼저 확정된(ID가 작은) 장르를 경유하는 경로가 선택된다.
- 최대 스텝 수 `maxSteps`: 기본 5 (출발 장르 제외), 유저가 3~8 사이로 조정 가능

| 구현체 | 방향 | 알고리즘 | 종료 조건 |
|---|---|---|---|
| `ROOTS` 뿌리 찾기 | 간선 **역방향** (파생 → 기원) | 탐욕 선택: 매 단계 가장 싼 부모 장르로 이동 | 부모가 없음 / maxSteps 도달 / 후보가 모두 싫어하는 장르 |
| `DESCENDANTS` 후손 찾기 | 간선 **정방향** (기원 → 파생) | 탐욕 선택: 매 단계 가장 싼 자식 장르로 이동. **막다른 장르면 지나온 장르를 최근 것부터 거슬러 올라가, 아직 안 간 자식이 있는 곳에서 이어 간다** `[구현됨]` | maxSteps 도달 / 지나온 어느 장르에도 갈 자식이 없음 |
| `BRIDGE` 두 장르 잇기 | **양방향** (간선 방향 무시) | **다익스트라 최단 경로** (비용 = 도착 장르의 개인 가중치 반영) | 목표 도달. maxSteps를 적용하지 않음 |
| `NEIGHBORS` 이웃 탐색 `[구현됨]` | 형제 장르 (같은 부모를 공유) | 1차: 출발 장르의 형제 → 모자라면 2차: 그 형제들의 형제. 차수 안의 순서는 비용 → 겹치는 부모 수(많을수록 가까움) → 등장 시기 → ID | maxSteps / 형제가 없음 |
| `CHRONOLOGICAL` 시대순 여행 `[구현됨]` | 정방향 | 출발 장르에서 닿는 후손만 모아 **위상 정렬**(모은 장르 중의 부모를 모두 거친 뒤에만 자식), 동시에 나올 수 있으면 등장 시기 → 비용 → ID | maxSteps / 더 갈 후손이 없음 |

- 이웃 탐색 예: 브릿팝 → 소울, 포크 록, 팝 록, 하드 록, 펑크 록 (브릿 인베이전·글램·인디의 다른 자식). 형제끼리는 간선으로 이어져 있지 않은 **옆 가지**다.
- 시대순 여행 예: 블루스 → 재즈(1910년대) → R&B → 두왑 → 로커빌리 → 브릿 인베이전. 소울은 부모(R&B, 두왑, 브릿 인베이전)가 모두 나온 뒤에 나온다.
- 두 방식 모두 싫어하는 장르는 거치지 않는다. 경로 재계산(5.5)도 다른 방식과 같은 규칙으로 동작한다.

예외
- `BRIDGE`에서 목표로 가는 경로가 없으면 `NO_PATH` 에러
- 출발 장르와 목표 장르가 같으면 `SAME_GENRE` 에러
- 결과가 출발 장르 1개뿐이면(더 갈 곳이 없음) `DEAD_END` 에러와 함께 다른 전략을 제안

### 5.3 ~~듣기 방식 `ListeningPlanStrategy`~~ `[제거됨]`

> 곡을 듣는 앱이던 시절, 하루에 들을 분량(맛보기: 하루 1장르 / 하루 1곡)을 정하던 전략이다.
> 소개 중심(17장)으로 바뀌면서 속도를 고를 이유가 사라져 **제거했다.** 하루 제한도 없애고,
> 오늘 3개 장르 이상 넘기면 **막지 않는 안내**("오늘 벌써 N개 장르를 지나왔어요. 쉬어 가도 좋아요")만 보여준다 (19.1).

### 5.4 반응 처리 `FeedbackHandler` `[Phase 1]`

> **19장에서 바뀜:** 스텝 완료는 이제 "다음 장르로" 버튼이 기본이다. 아래의 "모든 곡에 반응하면 판정"은 `ALL_RATED` 완료 방식에서만 쓰고, 기본(`MANUAL_NEXT`)에서는 곡 반응 다수결이 판정 질문의 **기본 선택값**이 된다.

곡마다 반응을 남긴다. 반응 종류마다 핸들러가 다르다. 이 부분도 enum → 핸들러 Map 구조의 전략이다.

| 반응 | 곡 단위 처리 |
|---|---|
| `LIKE` 좋아요 | 기록만 한다 |
| `DISLIKE` 별로 | **이유를 한 번 더 묻는다:** "곡이 별로예요"(`TRACK`) / "이 가수가 별로예요"(`ARTIST`). `ARTIST`면 가수 싫어함 선호를 저장한다 (16장) |
| `MEH` 애매함 | 같은 장르의 **예비곡 1곡**을 추가로 준다 (장르당 1회) |

장르 판정: 해당 장르에 배정된 곡에 모두 반응하면 판정한다.

| 판정 | 조건 (MEH와 **가수 이유 별로**는 중립으로 셈) | 효과 |
|---|---|---|
| 장르 좋아함 | LIKE 수 > DISLIKE 수, 그리고 LIKE ≥ 2 | 이웃 장르 가중치 × 0.8 → **남은 경로 재계산** |
| 장르 싫어함 | DISLIKE 수 > LIKE 수, 그리고 DISLIKE ≥ 2 | 해당 장르 가중치 × 3.0 → **남은 경로 재계산** |
| 중립 | 그 외 | 변화 없음 |

- 가수가 싫어서 누른 별로는 **장르 판정에 넣지 않는다.** Oasis가 싫어서 누른 별로 때문에 브릿팝이 싫어하는 장르로 판정되면 안 되기 때문이다.
- 판정 방식 자체도 `GenreVerdictPolicy`로 분리해 두면 나중에 교체할 수 있다. (예: 한 곡이라도 좋아요면 좋아함) `[Phase 2]`

### 5.5 경로 재계산 규칙

> 판정 시점은 19장("다음 장르로", 직전 판정 바꾸기)을 따른다. 재계산 규칙 자체는 아래와 같다.

- 재계산은 장르 판정이 **좋아함** 또는 **싫어함**일 때만 한다. (중립이면 그대로 진행)
- **좋아함:** 방금 끝난 스텝의 장르를 새 출발점으로 하여 같은 탐험 전략으로 다시 실행한다.
- **싫어함 → 가지 바꾸기:** 그 가지는 거기서 끊고, **직전 스텝의 장르로 돌아가** 다시 실행한다.
  - 직전 장르에서 더 갈 곳이 없으면(DEAD_END, NO_PATH) 방금 끝난 장르에서 이어서 계산한다.
  - 그래도 없으면: `ROOTS`·`DESCENDANTS`는 로드맵을 여기서 끝내고, `BRIDGE`는 기존 경로를 유지한다.
  - 결과적으로 싫어한 장르 다음 스텝은 싫어한 장르와 **직접 연결되지 않은 장르**(형제 가지)일 수 있다. 계보도에서는 이 구간에 선을 그리지 않는다.
  - 예: R&B → **두왑(싫어함)** → 소울 → … ⇒ R&B → 두왑 → 로커빌리 → 브릿 인베이전 → …
- 두 규칙의 역할 분담: 가지 바꾸기는 **지금 로드맵**에, 2.2의 가중치(싫어한 장르 ×3, 파생 장르 ×1.5)는 **다음 로드맵부터** 영향을 준다.
- 이미 지나온 장르는 방문 불가 목록에 넣는다.
- `ROOTS`와 `DESCENDANTS`는 남은 스텝 수(= maxSteps − 진행한 스텝 수)만큼만 다시 뽑는다.
- `BRIDGE`는 목표 장르까지 다시 최단 경로를 구한다.
- 새 경로가 기존과 같으면 아무것도 바꾸지 않는다. 다르면 로드맵의 `version`을 올리고 유저에게 "경로가 바뀌었어요"를 표시한다.

---

## 6. 같은 입력, 다른 결과 (시연 시나리오)

입력은 모두 같다: **"오아시스를 좋아함"** → 출발 장르 = 브릿팝

| 유저 | 전략 | 로드맵 |
|---|---|---|
| A | ROOTS | 브릿팝 → 브릿 인베이전 → 로커빌리 → 컨트리 (로커빌리의 부모 R&B·컨트리가 비용 동률 → 등장 시기가 빠른 컨트리. 컨트리는 부모가 없어 종료) |
| B | DESCENDANTS | 브릿팝 → 포스트 브릿팝 → … (데이터 범위에 따라) |
| C | BRIDGE (목표: 힙합) | 브릿팝 → 브릿 인베이전 → 소울 → 펑크(Funk) → 힙합 |
| D | BRIDGE (목표: 힙합) + **소울을 싫어함** | 브릿팝 → 브릿 인베이전 → 로커빌리 → R&B → 두왑 → 펑크(Funk) → 힙합 |

- C와 D는 **같은 전략, 같은 목표**인데 개인 가중치 때문에 경로가 다르다. 이게 이 앱의 기술적 핵심이다.
- 탐험 방식 선택 화면의 **미리보기** API로 A·B·C 결과를 한 화면에서 보여준다.

---

## 7. 초기 데이터 (MVP: 록 계보)

> **20장에서 바뀜:** 데이터는 이제 `MusicRoadmapServer/seed-data/main/seed/catalog.json` 한 파일에 있고, 재즈 계열이 추가되어 34개 장르·192곡이다. 아래는 처음 설계한 록 계보 초안이다.

> 공개된 록 계보도와 위키백과 장르 문서의 "기원 장르 / 파생 장르" 항목을 참고해 **직접 작성하는 초안**이다. 넣기 전에 한 번 검수한다.
> 계보도 **그림 자체는 앱에 쓰지 않는다.** (저작물) 장르 간 관계만 옮겨 적는다.

### 7.1 장르 (약 30개)
컨트리, 블루스, 재즈, 포크, R&B, 로커빌리, 두왑, 브릿 인베이전, 포크 록, 소울, 팝 록, 사이키델릭 록, 서던 록, 프로그레시브 록, 글램 록, 하드 록, 헤비메탈, 펑크 록, 뉴 웨이브, 그런지, 하드코어 펑크, 펑크(Funk), 디스코, 힙합, 인디 록, 브릿팝, 포스트 브릿팝, 얼터너티브 록

### 7.2 간선 초안 (기원 → 파생)

| 기원 | 파생 |
|---|---|
| 컨트리 | 포크, 로커빌리 |
| 블루스 | R&B, 재즈(병행 관계) |
| 포크 | 포크 록 |
| R&B | 로커빌리, 두왑, 소울, 하드 록 |
| 로커빌리 | 브릿 인베이전 |
| 두왑 | 소울, 펑크(Funk) |
| 브릿 인베이전 | 포크 록, 팝 록, 소울, 하드 록, 브릿팝 |
| 포크 록 | 뉴 웨이브 |
| 소울 | 사이키델릭 록, 펑크(Funk) |
| 사이키델릭 록 | 프로그레시브 록, 서던 록, 글램 록, 하드 록 |
| 팝 록 | 하드 록, 뉴 웨이브 |
| 하드 록 | 헤비메탈, 펑크 록, 그런지 |
| 헤비메탈 | 그런지 |
| 글램 록 | 펑크 록, 브릿팝 |
| 펑크 록 | 하드코어 펑크, 뉴 웨이브, 인디 록 |
| 뉴 웨이브 | 인디 록 |
| 인디 록 | 브릿팝, 얼터너티브 록 |
| 얼터너티브 록 | 그런지 |
| 브릿팝 | 포스트 브릿팝 |
| 펑크(Funk) | 디스코, 힙합 |
| 디스코 | 힙합 |

### 7.3 곡 데이터
- 장르당 **대표곡 3곡 + 예비곡 1곡** (약 120곡)
- 곡 정보: 제목, 아티스트, 발매 연도, 유튜브 검색어
- 아티스트: 장르당 2~3명과 **대표 장르 1개** 매핑 (출발점 선택용)

---

## 8. 데이터 모델

```
Genre ──< GenreEdge >── Genre
  │
  ├──< Artist
  └──< Track

User ──< Roadmap ──< RoadmapStep
  │          └──< ScheduledTrack
  ├──< ListeningLog
  └──< UserGenreWeight
```

### Genre
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| name | String | 영문명 (예: Britpop) |
| nameKo | String | 한글명 |
| originDecade | Integer | 등장 시기 (예: 1990) |
| description | Text | 2~3줄 소개 |
| posX / posY | Integer | 계보도 좌표 |
| mbid | String | MusicBrainz 장르 ID. nullable, 유니크 `[아카이브 대비]` |
| source | Enum | CURATED(직접 작성), MUSICBRAINZ `[아카이브 대비]` |

### GenreEdge
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| fromGenreId | FK | 기원 장르 |
| toGenreId | FK | 파생 장르 |
| baseCost | Double | 기본 1.0 |

- 유니크: (fromGenreId, toGenreId)

### Artist
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| name | String | |
| primaryGenreId | FK | 대표 장르 (출발점 판정용) |
| mbid | String | MusicBrainz 아티스트 ID. nullable, 유니크 `[아카이브 대비]` |
| source | Enum | CURATED, MUSICBRAINZ `[아카이브 대비]` |

### Track
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| genreId | FK | |
| artistId | FK | nullable. MVP는 artistName만 써도 됨 `[아카이브 대비]` |
| title / artistName | String | artistName은 화면 표시용 |
| releaseYear | Integer | |
| searchQuery | String | 유튜브 검색어 |
| role | Enum | MAIN(대표), SPARE(예비) |
| displayOrder | Integer | 장르 안에서의 순서 |
| mbid | String | MusicBrainz 녹음(Recording) ID. nullable, 유니크 `[아카이브 대비]` |
| source | Enum | CURATED, MUSICBRAINZ `[아카이브 대비]` |

### User
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| email / googleSub | String | 구글 로그인 도입 시 사용 |
| nickname | String | |
| accessToken | String | MVP 개발용 로그인 토큰 (9장 참고) |
| trackSelectionType | Enum | 곡 고르기 방식 (16장). 기본 EXCLUDE_DISLIKED |
| createdAt | LocalDateTime | |

### UserGenreWeight
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| userId / genreId | FK | 유니크 (userId, genreId) |
| multiplier | Double | 기본 1.0 (행이 없으면 1.0으로 간주) |

### Roadmap
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| userId | FK | |
| explorationType | Enum | ROOTS, DESCENDANTS, BRIDGE … |
| startGenreId | FK | |
| targetGenreId | FK | BRIDGE일 때만 |
| maxSteps | Integer | |
| status | Enum | IN_PROGRESS, COMPLETED, ABANDONED |
| currentStepIndex | Integer | |
| version | Integer | 재계산될 때마다 +1 |
| startedAt / completedAt | LocalDateTime | |

### RoadmapStep
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| roadmapId | FK | |
| stepIndex | Integer | 0 = 출발 장르 |
| genreId | FK | |
| status | Enum | PENDING, IN_PROGRESS, DONE, REPLACED(재계산으로 빠짐) |
| verdict | Enum | LIKED, DISLIKED, NEUTRAL (판정 후) |
| trackIds | List<Long> | 이 스텝에서 들을 곡. **스텝이 시작될 때** `TrackSelectionStrategy`가 고른다 (별도 테이블 `roadmap_step_track`) |
| spareTrackId | Long | 애매함일 때 추가로 줄 예비곡. nullable |
| spareUsed | Boolean | 예비곡을 이미 줬는지 |
| version | Integer | 이 스텝이 만들어진 로드맵 버전 |

- 곡을 로드맵 생성 시점이 아니라 **스텝이 시작될 때** 고르는 이유: 그 사이에 유저가 가수를 싫어하게 되면 반영하기 위함.
- 고른 곡이 하나도 없으면(모두 싫어하는 가수) 그 스텝은 **건너뛰고**(DONE, 중립) 다음 스텝으로 간다.
- 날짜별 일정은 두지 않는다 (듣기 방식 제거, 5.3). (v3 초안의 `ScheduledTrack` 테이블은 쓰지 않는다)

### ListeningLog
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| userId / trackId | FK | |
| roadmapStepId | FK | |
| feedback | Enum | LIKE, DISLIKE, MEH |
| dislikeReason | Enum | TRACK, ARTIST. feedback이 DISLIKE일 때만 값이 있음 |
| memo | String | 한 줄 감상 (선택, 100자) |
| listenedAt | LocalDateTime | |

- `[아카이브 대비]` MVP는 곡 단위 감상만 저장한다. 아카이브 단계에서 앨범·아티스트 단위 감상이 필요해지면 `targetType`(TRACK, ALBUM, ARTIST) + `targetId`를 쓰는 별도 `Review` 엔티티로 분리한다. ListeningLog는 로드맵 진행 기록으로 남겨 둔다.

### 확장 대비 공통 규칙 `[아카이브 대비]`
- `mbid`는 MVP에서는 비워 둬도 된다. 큐레이션 데이터에 알고 있는 ID만 채운다.
- 외부 데이터를 가져올 때는 `mbid`로 **중복을 확인한 뒤 저장**한다 (있으면 갱신, 없으면 생성).
- 외부에서 사라진 데이터는 지우지 않고 `deprecated = true`(Boolean, 기본 false)로 표시한다. `Genre`, `Artist`, `Track`에 이 필드를 둔다. (15.2 규칙 3)
- `source = CURATED`인 데이터는 외부 동기화로 **덮어쓰지 않는다.** 직접 다듬은 설명이나 장르 매핑을 지키기 위함.
- **저장 금지:** 가사, 음원, 매체 리뷰 원문. 외부 링크로만 연결한다.

---

## 9. API (Phase 1)

| Method | URL | 설명 |
|---|---|---|
| POST | /api/auth/dev-login | **MVP 개발용 로그인.** 닉네임으로 유저를 만들거나 찾아서 토큰 발급 |
| POST | /api/auth/google | 구글 로그인 → JWT `[구글 로그인 도입 시]` |
| POST | /api/auth/refresh | 토큰 재발급 `[구글 로그인 도입 시]` |
| GET | /api/catalog | 전체 계보(장르·간선·좌표) + 곡 + 아티스트. 데이터가 작아서 한 번에 받는다 |
| GET | /api/artists?q= | 아티스트 검색 (출발점 선택) |
| POST | /api/roadmaps/preview | 같은 출발점으로 **여러 전략의 경로를 비교** (저장 안 함) |
| POST | /api/roadmaps | 로드맵 생성 (출발점, 탐험 방식, 목표 장르, maxSteps) |
| GET | /api/roadmaps/current | 진행 중인 로드맵 |
| GET | /api/roadmaps/{id} | 로드맵 상세 (스텝, 버전) |
| POST | /api/roadmaps/{id}/abandon | 로드맵 그만두기 |
| GET | /api/today | 오늘 들을 곡 목록 |
| POST | /api/listens | 곡 반응 + 감상 (장르 판정 → 재계산이 일어날 수 있음) |
| GET | /api/me/roadmaps | 내 로드맵 전체 (계보도 표시용) |
| GET | /api/me/logs | 내 반응·감상 기록 |
| GET | /api/me/preferences | 내 선호 목록 (16장) |
| PUT | /api/me/preferences | 선호 저장 `{targetType, targetId, preference}` |
| DELETE | /api/me/preferences/{targetType}/{targetId} | 선호 삭제 |
| PATCH | /api/me/settings | 설정 변경 `{trackSelectionType}` |
| POST | /api/debug/skip-day | **local·h2 프로필 전용.** 하루가 지난 것처럼 서버 시계를 옮긴다 ("오늘 넘긴 장르 수" 확인용) |

- 인증: 요청 헤더 `Authorization: Bearer {accessToken}`. MVP는 개발용 로그인으로 받은 토큰을 쓰고, 구글 로그인 도입 시 JWT로 바꾼다.
- `POST /api/listens` 요청: `{ "trackId": 12, "feedback": "DISLIKE", "dislikeReason": "ARTIST", "memo": "..." }`

### 응답 예: `POST /api/listens` 결과 경로가 바뀐 경우
```json
{
  "genreVerdict": "DISLIKED",
  "roadmapChanged": true,
  "roadmapVersion": 2,
  "nextSteps": ["R&B", "두왑", "펑크(Funk)", "힙합"]
}
```

### 에러 형식
```json
{ "code": "NO_PATH", "message": "선택한 두 장르를 잇는 경로가 없습니다." }
```

| 코드 | 상황 |
|---|---|
| NO_PATH | BRIDGE에서 경로 없음 |
| SAME_GENRE | 출발 장르 = 목표 장르 |
| DEAD_END | 더 갈 장르가 없음 |
| ROADMAP_ALREADY_IN_PROGRESS | 진행 중인 로드맵이 이미 있음 |
| UNSUPPORTED_EXPLORATION | 알 수 없는 전략 값 |
| UNAUTHORIZED | 토큰이 없거나 잘못됨 |
| TARGET_NOT_FOUND | 선호 대상(장르·가수·곡)이 없음 |
| TRACK_NOT_AVAILABLE | 오늘 반응할 수 없는 곡 |
| ALREADY_RATED | 이미 반응한 곡 |

---

## 10. 테스트 케이스 (전략 단위 테스트)

테스트용 작은 그래프:
```
블루스 → R&B → 로커빌리 → 브릿 인베이전 → 브릿팝
            R&B → 소울 → 펑크(Funk) → 힙합
            R&B → 두왑 → 펑크(Funk)
                  브릿 인베이전 → 소울
```

| # | 대상 | 조건 | 기대 결과 |
|---|---|---|---|
| 1 | ROOTS | 출발 브릿팝, 가중치 기본 | 브릿팝 → 브릿 인베이전 → 로커빌리 → R&B → 블루스 |
| 2 | ROOTS | maxSteps = 2 | 브릿팝 → 브릿 인베이전 → 로커빌리 |
| 3 | ROOTS | 출발 블루스 | DEAD_END |
| 4 | DESCENDANTS | 출발 R&B, 로커빌리 싫어함 | R&B → 두왑 → 펑크 → 힙합 (로커빌리 제외, 소울과 두왑은 동률이라 등장 시기가 빠른 두왑) |
| 5 | DESCENDANTS | 자식 2개 비용 동률 | originDecade가 빠른 장르 선택 |
| 6 | BRIDGE | 브릿팝 → 힙합, 기본 | 브릿팝 → 브릿 인베이전 → 소울 → 펑크 → 힙합 |
| 7 | BRIDGE | 브릿팝 → 힙합, 소울 가중치 3.0 | 브릿팝 → 브릿 인베이전 → 로커빌리 → R&B → 두왑 → 펑크 → 힙합 (싫어하는 장르 제외 규칙) |
| 8 | BRIDGE | 끊어진 두 장르 | NO_PATH |
| 9 | 가중치 | 같은 장르를 5번 싫어함 | multiplier = 10.0 (상한) |
| 9-1 | 가중치 | 소울을 싫어함 | 소울 3.0, 파생 장르(사이키델릭 록·펑크) 1.5, 기원 장르(R&B·두왑) 1.0 |
| 10 | 장르 판정 | LIKE 2, DISLIKE 1 | LIKED |
| 11 | 장르 판정 | LIKE 1, MEH 2 | NEUTRAL |
| 12 | 재계산 | 3번째 스텝에서 싫어함 판정 | 이미 지나온 스텝은 유지, version +1 |
| 12-1 | 가지 바꾸기 | 실제 시드 데이터: R&B에서 후손 찾기(maxSteps 4), 두왑을 싫어함 | R&B → 두왑 → 소울 → 사이키델릭 록 → 프로그레시브 록 ⇒ R&B → 두왑 → 로커빌리 → 브릿 인베이전 → 포크 록 |
| 15 | Resolver | 모든 enum 값 | 각각 구현체가 존재 (빠진 전략 방지) |

- 전략은 모두 **스프링 없이 순수 자바로 테스트**할 수 있게 만든다. (그래프와 가중치를 인자로 받음)

---

## 11. 패키지 구조 (서버 `MusicRoadmapServer`, 구현됨)

```
com.project.musicroadmap
├── auth/          User, 개발용 로그인(dev-login), @LoginUser 토큰 해석, UserService(설정)
├── common/        ErrorCode, BusinessException, 전역 예외 처리, AppClock(서울 시간·skip-day)
├── genre/         [참조 레이어] Genre, GenreEdge, Artist, Track, GenreGraph(메모리 그래프), 카탈로그 API
│   └── seed/      SeedData(7장 초기 데이터), SeedDataLoader(빈 DB에 넣기)
├── roadmap/       RoadmapService(5장 규칙), RoadmapController, DTO
│   ├── domain/        Roadmap, RoadmapStep, 상태 enum
│   ├── exploration/   ExplorationStrategy + ROOTS·DESCENDANTS·BRIDGE + Resolver
│   ├── completion/    StepCompletionPolicy + MANUAL_NEXT·ALL_RATED + Resolver (19장)
│   └── selection/     TrackSelectionStrategy + EXCLUDE_DISLIKED·FAVORITE_ARTIST_FIRST + Resolver (16장)
├── listening/     [개인 레이어] ListeningLog, GenreVerdictPolicy(다수결, 가수 이유 별로는 중립)
├── weight/        [개인 레이어] UserGenreWeight, UserWeights(2.2 가중치 규칙), WeightService
├── preference/    [개인 레이어] UserPreference, PreferenceService (16장)
│   └── handler/       PreferenceHandler + GENRE·ARTIST·TRACK + Resolver
├── me/            /api/me/* (내 로드맵·기록·선호·설정)
└── debug/         /api/debug/skip-day (local 프로필 전용)
```

- 전략 인터페이스는 모두 **enum + `@Component` 구현체 + Resolver(스프링이 `List<구현체>` 주입)** 구조다 (5.0).
- 테스트: 순수 자바 전략 테스트(`ExplorationStrategyTest`), H2 서비스 흐름 테스트(`RoadmapServiceTest`), HTTP 테스트(`ApiTest`).

## 12. 개발 단계

| Phase | 범위 |
|---|---|
| **1 (MVP)** | 로그인, 록 계보 데이터(약 30장르·120곡), 출발점 2종(GENRE, ARTIST), 탐험 3종(ROOTS, DESCENDANTS, BRIDGE), 미리보기, 반응 처리, 가중치, 재계산, 나의 계보도 |
| **2** | ✅ 탐험 2종 추가(NEIGHBORS, CHRONOLOGICAL) / 보류: 장르 판정 정책 교체(구조만 있음, "다음 장르로" 도입 후 판정은 유저가 직접 고르고 다수결은 기본값으로만 쓰여 우선순위 낮음) / **제외:** 간선 비용 차등(영향 강도가 주관적이라 근거가 약하고, 개인화는 이미 유저 가중치가 맡음), MULTI_ARTIST(소개 중심 흐름에서 출발점은 하나가 명확하고, 먼 장르 조합은 두 장르 잇기가 해결) |
| **3** | ✅ 재즈 계열 6장르 추가 + 기존 장르 가수 4명 → 6명 / ✅ 일렉트로닉 6장르 + 힙합 세부 5장르 추가. 두 번 모두 **코드 수정 없이 데이터 파일만 바뀐 것을 확인** (20장) / ✅ 후손 찾기 막다른 곳 되돌아가기 (5.2) |
| **4** | 계보도 이미지 공유, 다른 유저의 로드맵 따라가기 |
| **5** | 뮤직 아카이브 확장 (14장 참고) |

## 13. 확장 시나리오 (OCP 검증용)

| 시나리오 | 바뀌는 것 | 바뀌지 않는 것 |
|---|---|---|
| 새 탐험 방식 추가 (예: 랜덤 산책) | enum 1개 + 클래스 1개 | 서비스, 컨트롤러, 다른 전략 |
| **실제 사례: 이웃 탐색·시대순 여행 추가** | 서버: `ExplorationType`에 값 2개 + `NeighborsStrategy`·`ChronologicalStrategy` 클래스. 앱: enum 값 2개(화면 문구) + 같은 이름의 클래스 2개 | `RoadmapService`, `RoadmapController`, 앱 화면 코드 모두 그대로. 미리보기 API와 탐험 화면에 **자동으로** 새 카드가 나옴 |
| 새 스텝 완료 방식 추가 | enum 1개 + 클래스 1개 | 로드맵 진행 로직 |
| 재즈 계열 추가 | 데이터(장르·간선·곡)만 | 코드 전체 |
| 장르 판정 규칙 변경 | GenreVerdictPolicy 구현체 교체 | FeedbackHandler, 재계산 로직 |

---

## 14. 확장 계획: 뮤직 아카이브 `[Phase 5]`

> 공개 음악 데이터를 **뼈대**로 가져오고, 그 위에 **나의 기록과 해석**을 쌓는 개인 음악 아카이브.
> MVP(로드맵)를 완성한 뒤 진행한다. 지금은 8장의 `[아카이브 대비]` 필드만 미리 넣어 둔다.

### 14.1 데이터 출처

| 데이터 | 출처 | 사용 조건 |
|---|---|---|
| 아티스트, 앨범, 곡, 멤버, 레이블, 아티스트 간 관계 | MusicBrainz | 핵심 데이터 CC0 |
| 앨범 커버 | Cover Art Archive | 이미지마다 권리가 달라 **링크로만 표시** |
| 장르·아티스트 소개 글 | 위키백과 | CC BY-SA: 출처 표기 필수 |
| 장르 계보 | 자체 큐레이션 | 자체 데이터 |

### 14.2 가져오기 방식 — 아티스트 검색 확장 `[구현됨]`
- MusicBrainz 전체(수십 GB 덤프)를 가져오지 않는다. 유저가 **선택한 대상만** 가져와서 저장한다 (필요할 때 가져오는 캐시).
- **검색 결과는 저장하지 않는다.** 목록에서 유저가 하나를 선택했을 때 상세 조회 후 저장한다.
- 데이터 출처마다 `MusicDataSource` 인터페이스의 구현체를 둔다 (MusicBrainz / 위키백과 / 관리자 수동 입력). 출처가 늘어도 서비스 코드는 바뀌지 않는다.

#### 호출 흐름
```
[앱] 아티스트 검색
  ↓
[서버] ① 우리 DB 검색
       ② MusicBrainz 검색 API 호출 → 목록만 반환 (저장 안 함)
  ↓
[앱] 목록에서 선택
  ↓
[서버] ③ DB에 있고 갱신 30일 이내 → DB 값 반환
       ④ 없거나 오래됨 → MusicBrainz 상세 조회 → upsert
       ⑤ 위키백과 요약 조회 → 출처 URL과 함께 저장
  ↓
[앱] 아티스트 페이지 (앨범 커버는 URL로 직접 로드)
```

#### 사용하는 API
| 데이터 | 호출 | 비고 |
|---|---|---|
| 아티스트 검색 | `GET https://musicbrainz.org/ws/2/artist?query={키워드}&fmt=json` | score 순 정렬 |
| 아티스트 상세 | `GET https://musicbrainz.org/ws/2/artist/{mbid}?inc=genres+artist-rels+url-rels&fmt=json` | 장르 투표, 아티스트 관계, 외부 링크 |
| 디스코그래피 | `GET https://musicbrainz.org/ws/2/release-group?artist={mbid}&type=album&limit=100&fmt=json` | 100개 초과 시 `offset`으로 페이지 이동 |
| 앨범 커버 | `https://coverartarchive.org/release-group/{mbid}/front-250` | **이미지 저장 안 함.** URL을 앱에 넘겨 Coil로 로드 |
| 소개 글 | `url-rels`의 위키데이터 링크 → 한국어 위키백과 문서 제목 → `GET https://ko.wikipedia.org/api/rest_v1/page/summary/{제목}` | 한국어 문서가 없으면 영어 위키백과. 출처 URL 저장 필수 |

#### MusicBrainz 이용 규칙
- 요청은 **IP당 초당 1회**. 초과하면 `503` 응답, 반복하면 차단될 수 있다.
- **User-Agent 헤더 필수:** `앱이름/버전 ( 연락처 )` 형식.
- 서버 1대일 때는 요청 사이 간격을 두는 단순 구현(`synchronized` + 마지막 요청 시각)으로 충분하다. 서버가 여러 대가 되면 Resilience4j RateLimiter 또는 요청 큐로 바꾼다.
- `503`을 받으면 일정 시간 뒤 최대 2회 재시도한다.

#### 구현 메모 (Spring Boot 4)
- HTTP 클라이언트는 `RestClient`를 쓴다. baseUrl과 User-Agent를 Bean 설정에서 고정한다.
- 응답 DTO는 `record` + `@JsonIgnoreProperties(ignoreUnknown = true)`로 필요한 필드만 받는다.
- 저장은 `findByMbid` → 없으면 생성, 있으면 갱신. 단, `source = CURATED`인 데이터는 덮어쓰지 않는다.

아래 코드는 흐름을 보여주는 **뼈대**다. 실제 구현 시 예외 처리와 재시도를 보강한다.

**① RestClient 설정**
```java
@Configuration
public class MusicBrainzConfig {
    @Bean
    RestClient musicBrainzRestClient() {
        return RestClient.builder()
                .baseUrl("https://musicbrainz.org/ws/2")
                .defaultHeader(HttpHeaders.USER_AGENT, "MusicRoadmap/0.1 ( 연락처 이메일 )")
                .build();
    }
}
```

**② 요청 제한을 지키는 클라이언트** (서버 1대 기준)
```java
@Component
public class MusicBrainzClient {
    private final RestClient restClient;
    private long lastRequestAt = 0;

    public MusicBrainzClient(RestClient musicBrainzRestClient) {
        this.restClient = musicBrainzRestClient;
    }

    // 초당 1회 제한: 요청 사이에 1.1초 이상 간격을 둔다
    private synchronized void throttle() {
        long wait = 1100 - (System.currentTimeMillis() - lastRequestAt);
        if (wait > 0) {
            try { Thread.sleep(wait); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        lastRequestAt = System.currentTimeMillis();
    }

    public MbArtistSearchResponse searchArtists(String keyword) {
        throttle();
        return restClient.get()
                .uri(b -> b.path("/artist").queryParam("query", keyword).queryParam("fmt", "json").build())
                .retrieve()
                .body(MbArtistSearchResponse.class);
    }

    public MbArtist getArtist(String mbid) {
        throttle();
        return restClient.get()
                .uri(b -> b.path("/artist/{mbid}")
                        .queryParam("inc", "genres+artist-rels+url-rels")
                        .queryParam("fmt", "json")
                        .build(mbid))
                .retrieve()
                .body(MbArtist.class);
    }
}
```

**③ 응답 DTO** (필요한 필드만)
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record MbArtistSearchResponse(List<MbArtist> artists) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record MbArtist(String id, String name, String country, List<MbGenre> genres) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record MbGenre(String id, String name, int count) {}   // count = 득표 수
```

**④ 저장 (upsert)**
```java
@Transactional
public Artist saveFromMusicBrainz(MbArtist mb) {
    Artist artist = artistRepository.findByMbid(mb.id())
            .orElseGet(() -> Artist.fromMusicBrainz(mb));   // 없으면 새로 생성

    if (artist.getSource() != Source.CURATED) {
        artist.updateFrom(mb);   // 직접 작성한 데이터는 덮어쓰지 않음
    }
    return artistRepository.save(artist);
}
```

#### 구현 결과 (서버 `external/musicbrainz`, `genre/mapping`, `genre/importer`)
| Method | URL | 설명 |
|---|---|---|
| GET | /api/artists/external?q= | MusicBrainz 검색 후보 10개 (저장 안 함). 이미 가져온 아티스트는 `importedArtistId` 표시 |
| POST | /api/artists/import `{mbid}` | 상세 조회 → 장르 매핑 → 저장. 이미 있으면 외부 호출 없이 DB 값 반환 |

- 같은 이름의 **큐레이션 아티스트**가 있으면 새로 만들지 않고 `mbid`만 연결한다 (직접 다듬은 장르 유지, 15.2).
- 동명이인이 있을 수 있어 `Artist.name`은 유니크가 아니다. 구분은 `mbid`로 한다.
- 외부 호출은 **트랜잭션 밖**에서 하고, DB 작업만 짧은 트랜잭션(`TransactionTemplate`)으로 묶는다. 1초 이상 걸리는 호출 동안 DB 연결을 잡고 있지 않기 위함.
- 검색어의 특수문자(예: `AC/DC`)는 MusicBrainz 검색 문법으로 해석되지 않게 이스케이프한다.
- 앱: 아티스트 검색 결과 아래 **"MusicBrainz에서 더 찾기"** → 후보 선택 → 장르가 연결되면 바로 탐험 화면, 없으면 장르 탭으로 바꾸고 직접 고르게 안내.
- **Spring Boot 4 주의:** `RestClient.Builder` 자동 설정이 `spring-boot-starter-restclient`로 분리되어 있어 의존성을 따로 추가해야 한다.
- 설정: `app.musicbrainz.user-agent`의 연락처는 환경변수 `MUSICBRAINZ_CONTACT`로 넣는다.

### 14.2.1 장르 매핑 전략 `GenreMappingStrategy` `[구현됨: MOST_VOTED, MOST_SPECIFIC]`
MusicBrainz 아티스트에는 유저 투표로 붙은 장르 목록이 있다. (예: britpop 12표, rock 8표, alternative rock 5표)
이것을 **우리 계보도의 장르 하나**로 연결해야 로드맵의 출발점으로 쓸 수 있다.

| 구현체 | 규칙 |
|---|---|
| `MOST_VOTED` 최다 득표 | 계보도에 있는 장르 중 득표가 가장 많은 것 |
| `MOST_SPECIFIC` 구체적인 장르 우선 | 넓은 장르(rock)보다 좁은 장르(britpop) 우선. 계보도에서 더 깊은(조상이 많은) 장르를 구체적이라고 본다 |
| `CURATED_FIRST` 큐레이션 우선 | 관리자가 아티스트별로 지정한 장르가 있으면 사용하고, 없으면 다른 전략으로 넘김 |

- 필요 데이터: **`GenreAlias` 대응표** (MusicBrainz 장르명 → 우리 장르 ID). 각 장르의 영문 이름 + 추가 별칭(예: `hip-hop`, `rap` → 힙합)을 시드 데이터로 넣는다. `rock`, `pop`처럼 계보도에 없는 넓은 장르는 넣지 않는다.
- 같은 우리 장르로 모이는 외부 장르는 **투표를 합친다.** (예: Arctic Monkeys = indie rock 19 + garage rock revival 7 + post-punk revival 7 → 인디 록 33)
- 전략에 따라 결과가 다르다: Arctic Monkeys는 `MOST_VOTED`면 **인디 록**, `MOST_SPECIFIC`이면 **얼터너티브 록**(인디 록의 파생이라 더 구체적). 설정 `app.genre-mapping.strategy`로 고른다 (기본 MOST_VOTED).
- `CURATED_FIRST`는 관리자 기능이 생기면 추가한다.
- 계보도에 대응되는 장르가 하나도 없으면 `primaryGenreId`를 비워 두고, 출발점으로 쓸 때 유저에게 장르를 직접 고르게 한다.

### 14.2.2 MVP 데이터에 활용
- 큐레이션 곡 약 120개의 `mbid`를 채울 때도 같은 검색 API를 쓴다.
- 한 번만 실행하는 스크립트로 후보를 뽑고, 사람이 확인해서 넣는다. (요청 제한 때문에 120곡이면 2분 이상 걸림)

### 14.3 추가 기능
| 기능 | 내용 |
|---|---|
| 아티스트·앨범 페이지 | 기본 정보, 멤버, 디스코그래피, 내 감상 |
| 앨범·아티스트 감상 | `Review` 엔티티 (8장 ListeningLog 비고 참고) |
| 아티스트 그래프 탐험 | 5.2의 탐험 전략을 아티스트 관계(영향, 멤버 이동, 협업)에도 적용. 예: "라디오헤드에서 비욘세까지 잇기" |
| 아카이브 보기 방식 | 연대기순 / 장르 지도 / 아티스트 관계망. 보기 방식도 전략으로 분리 |

### 14.4 규모가 커질 때 바뀌는 것
| 항목 | MVP | 아카이브 |
|---|---|---|
| 그래프 | 장르 수백 개, 전체를 메모리에 적재 | 아티스트 그래프는 커질 수 있음 → 탐색 깊이 제한(예: 3단계) 또는 DB 부분 조회 |
| 검색 | 단순 LIKE 검색 | MySQL FULLTEXT → 필요하면 Elasticsearch |

---

## 15. 데이터 레이어 원칙: 참조 레이어 + 개인 레이어

> **공개 데이터는 가져와서 참조만 하고, 나의 기록과 해석은 그 위에 따로 쌓는다.**
> 공개 데이터를 유저가 직접 고치지 않는다. 유저의 해석은 공개 데이터를 **덮어쓰지 않고 겹쳐서(overlay)** 보여준다.

### 15.1 두 레이어

```
┌──────────────────────────────────────────────┐
│ 개인 레이어 (유저마다 다름, 유저가 쓰고 고침)          │
│  UserGenreWeight · ListeningLog · Review ·    │
│  UserTag · UserGenreOverride · UserEdge       │
└──────────────────────▲───────────────────────┘
                       │ 내부 ID로 참조 (읽기 전용)
┌──────────────────────┴───────────────────────┐
│ 참조 레이어 (모든 유저 공통, 관리자·동기화만 씀)        │
│  Genre · GenreEdge · Artist · Track · Album   │
│  출처: CURATED(직접 큐레이션) / MUSICBRAINZ 등      │
└──────────────────────────────────────────────┘
```

| 구분 | 참조 레이어 | 개인 레이어 |
|---|---|---|
| 내용 | 장르, 계보, 아티스트, 앨범, 곡 | 가중치, 반응, 감상, 태그, 나만의 분류와 연결 |
| 누가 쓰나 | 관리자, 외부 데이터 동기화 | 해당 유저만 |
| 유저 권한 | **읽기만** | 읽기·쓰기·삭제 |
| 공개 범위 | 모두 같음 | 기본 비공개 (공유는 Phase 4) |

### 15.2 규칙
1. **참조 레이어는 유저 요청으로 수정하지 않는다.** 유저 API에는 참조 레이어의 쓰기 엔드포인트가 없다.
2. **개인 레이어는 참조 레이어를 내부 ID(`Long id`)로 가리킨다.** 외부 ID(`mbid`)로 가리키지 않는다. 외부 데이터가 바뀌거나 병합돼도 내 기록이 끊기지 않게 하기 위함이다.
3. **외부 데이터가 사라져도 참조 레이어 행은 지우지 않는다.** `deprecated = true`로 표시만 한다. 내 기록이 가리키는 대상이 사라지지 않도록 한다.
4. **화면에 보여줄 때는 "참조 + 내 해석"을 합친 결과**를 준다. 내 해석이 있으면 내 해석을 우선 표시하되, 원래 값도 함께 볼 수 있게 한다.
   - 예: 계보도상 "브릿팝"인 아티스트를 내가 "인디 록"으로 분류했다면 → 내 화면에는 "인디 록 (공식 분류: 브릿팝)"
5. **로드맵 계산은 기본적으로 참조 그래프 + 내 가중치**로 한다. 내가 추가한 연결(`UserEdge`)을 계산에 넣을지는 유저가 켜고 끈다 (기본 꺼짐).
6. **개인 레이어 삭제는 그 유저 데이터만 지운다.** 회원 탈퇴 시 개인 레이어 전체를 지우고, 참조 레이어는 그대로 둔다.

### 15.3 개인 레이어 엔티티

| 엔티티 | 내용 | 단계 |
|---|---|---|
| `UserGenreWeight` | 장르별 개인 가중치 (2.2) | Phase 1 |
| `ListeningLog` | 곡 반응과 한 줄 감상 (8장) | Phase 1 |
| `UserPreference` | 장르·가수·곡 단위 좋아함/싫어함 (16장) | Phase 1 |
| `Review` | 앨범·아티스트 단위 감상 (`targetType` + `targetId`) | Phase 5 |
| `UserTag` | 내가 붙인 태그 (예: "비 오는 날", "운동할 때") | Phase 5 |
| `UserGenreOverride` | 아티스트·곡을 내가 다르게 분류 (`targetType`, `targetId`, `genreId`, `note`) | Phase 5 |
| `UserEdge` | 내가 느낀 장르·아티스트 사이 연결 (`fromId`, `toId`, `note`) — "이 둘은 이어져 있다"는 나만의 해석 | Phase 5 |

- 모든 개인 레이어 엔티티는 `userId`를 가지고, 조회 시 **항상 `userId` 조건**을 건다.

### 15.4 합쳐서 보여주는 방식 (구현 메모)
- 서비스 계층에서 참조 데이터와 개인 데이터를 따로 조회한 뒤 **DTO에서 합친다.** 엔티티를 합치거나 참조 엔티티에 개인 필드를 추가하지 않는다.
- 로드맵 계산용 그래프는 참조 그래프(메모리, 공통) 위에 유저의 가중치·연결을 **요청마다 덧씌운 뷰**로 만든다. 공통 그래프 객체는 절대 수정하지 않는다 (여러 유저가 동시에 쓰기 때문).

```java
// 예: 아티스트 상세 = 참조 + 내 해석
public ArtistDetailResponse getArtistDetail(Long userId, Long artistId) {
    Artist artist = artistRepository.findById(artistId).orElseThrow();              // 참조 레이어
    Optional<UserGenreOverride> override =
            overrideRepository.findByUserIdAndTarget(userId, TargetType.ARTIST, artistId); // 개인 레이어
    List<Review> myReviews = reviewRepository.findByUserIdAndTarget(userId, TargetType.ARTIST, artistId);

    return ArtistDetailResponse.of(artist, override, myReviews);   // DTO에서 합침
}
```

---

## 16. 다단계 선호: 장르 · 가수 · 곡 `[Phase 1]`

> 유저는 **장르는 좋아해도 그 장르의 특정 가수는 싫어할 수 있다.** (예: 브릿팝은 좋은데 Oasis는 별로)
> 이것은 "유저마다 입력 항목이 다른" 문제가 아니다. 입력 형식은 모두 같고(**무엇을 좋아한다/싫어한다**), 사람마다 **행이 다를 뿐**이다.
> 그래서 리플렉션이나 동적 필드 없이 **선호 테이블 하나**로 푼다.

### 16.1 `UserPreference`
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | PK |
| userId | FK | |
| targetType | Enum | GENRE, ARTIST, TRACK (나중에 ALBUM) |
| targetId | Long | 대상의 내부 ID |
| preference | Enum | LIKE, DISLIKE |
| updatedAt | LocalDateTime | |

- 유니크: (userId, targetType, targetId). 같은 대상에 다시 저장하면 덮어쓴다.
- `targetId`에는 DB 외래키를 걸 수 없다 (대상 종류마다 가리키는 테이블이 다름). 대상 존재 여부는 `PreferenceHandler`가 서비스 계층에서 검증한다.
- 장르 가중치(`UserGenreWeight`)는 경로 계산용 **숫자**, `UserPreference`는 유저가 밝힌 **명시적 선호**로 역할이 다르다.

### 16.2 우선순위: 더 구체적인 쪽이 이긴다
> **곡 > 가수 > 장르**

| 상황 | 결과 |
|---|---|
| 브릿팝 좋아함 + Oasis 싫어함 | 브릿팝 스텝에서 Oasis 곡은 빼고 다른 가수 곡을 준다 |
| Oasis 싫어함 + Wonderwall 좋아함 | Wonderwall은 준다 (곡 선호가 가수 선호보다 우선) |
| 곡 싫어함 | 그 곡은 가수 선호와 상관없이 주지 않는다 |
| 선호 없음 | 기본 순서대로 |

### 16.3 선호가 생기는 경로
| 경로 | 저장되는 선호 |
|---|---|
| 곡에 "별로" → "이 가수가 별로예요" 선택 | ARTIST / DISLIKE |
| 곡 카드의 "이 가수 좋아요" | ARTIST / LIKE |
| 선호 API로 직접 저장·삭제 | 요청한 대로 |

### 16.4 전략 지점
| 전략 | 구현체 | 역할 |
|---|---|---|
| `TrackSelectionStrategy` 곡 고르기 | `RANDOM` 장르(키)의 가수 배열에서 무작위, 좋아하는 가수는 앞에 (새 유저 기본) / `EXCLUDE_DISLIKED` 싫어하는 곡·가수만 빼고 기본 순서 / `FAVORITE_ARTIST_FIRST` 좋아하는 가수 곡을 먼저 | 스텝이 시작될 때 대표곡 3곡 + 예비곡 1곡을 고른다. 빠진 자리는 예비곡이 채운다 |
| `PreferenceHandler` 대상별 처리 | `GENRE` / `ARTIST` / `TRACK` | 대상 존재 검증 + 저장 후 후속 처리. **장르 선호는 가중치에도 반영**한다 (좋아함 → 이웃 ×0.8, 싫어함 → ×3.0 + 파생 ×1.5) |
| `GenreVerdictPolicy` 장르 판정 | 다수결 | 가수 이유 별로는 중립으로 센다 (5.4) |

- 곡 고르기 방식은 유저 설정(`User.trackSelectionType`)으로 고른다. 새 유저의 기본값은 설정 `app.track-selection.default`(기본 `RANDOM`).
- 모든 방식에서 **싫어하는 곡·가수는 먼저 빠진다.** `RANDOM`은 남은 가수 중 좋아하는 가수를 앞에 두고(그 안에서도 섞음) 나머지를 섞는다.
- 스텝이 시작될 때 **한 번 뽑아 저장**하므로 다시 열어도 순서가 바뀌지 않는다. 장르 상세의 핵심 가수 목록은 화면에 들어올 때마다 섞어 보여준다.
- 테스트는 결과가 같아야 하므로 난수 생성기의 씨앗을 고정한다. (주의: `java.util.Random`을 씨앗 0, 1, 2…로 매번 새로 만들면 첫 결과들이 비슷해 치우친다. 생성기 하나를 이어서 쓴다)
- 선호를 삭제해도 이미 반영된 장르 가중치는 되돌리지 않는다.

### 16.5 예시
유저 A: 브릿팝 좋아함, Oasis 싫어함

1. 브릿팝 스텝의 후보: Wonderwall(Oasis), Parklife(Blur), Common People(Pulp), 예비곡 Animal Nitrate(Suede)
2. `EXCLUDE_DISLIKED`가 Oasis를 뺀다 → 대표곡 **Parklife, Common People, Animal Nitrate**, 예비곡 없음
3. 세 곡 모두 좋아요 → 브릿팝 "좋아함". Oasis 때문에 판정이 흔들리지 않는다.

### 16.6 테스트 케이스
| # | 조건 | 기대 결과 |
|---|---|---|
| 1 | Oasis 싫어함, 브릿팝 스텝 시작 | 대표곡에 Wonderwall 없음, Animal Nitrate가 대표곡으로 올라옴 |
| 2 | Oasis 싫어함 + Wonderwall 좋아함 | Wonderwall 포함 |
| 3 | 별로(가수 이유) 2 + 좋아요 1 | 장르 판정: 중립 (별로로 세지 않음) |
| 4 | 별로(곡 이유) 2 + 좋아요 1 | 장르 판정: 싫어함 |
| 5 | 한 장르의 모든 가수를 싫어함 | 그 스텝은 건너뛰고 다음 스텝 시작 |
| 6 | `FAVORITE_ARTIST_FIRST` + Suede 좋아함 | Animal Nitrate가 첫 번째 곡 |
| 7 | 없는 가수 ID로 선호 저장 | TARGET_NOT_FOUND |

---

## 17. 소개 중심 전환과 재생 방식 전략 `[구현됨]`

> 음악은 다른 재생 앱이 많으니, 이 앱은 **"이 장르를 만든 사람들과 그들의 대표곡"을 소개**하는 데 집중한다.

### 17.1 구조
```
장르 ── 핵심 가수 (GenreArtist: 이 장르에서의 역할 한 줄 소개, 순서)
           └── 대표곡 (Track)
```
- 한 가수가 여러 장르에 나오면 장르마다 소개가 다르다 (예: Bob Dylan — 포크 / 포크 록, R.E.M. — 인디 록 / 얼터너티브 록).
- 소개 112개는 직접 쓴 글이다 (위키백과 문장을 옮기지 않음 → 출처 표기 의무 없음).
- 오늘 화면의 곡 카드는 **가수 카드**(가수 이름, 역할 소개, "대표곡 · 곡명 (연도)")로 바뀐다.

### 17.2 재생 방식 `PlaybackStrategy`
| 구현체 | 동작 |
|---|---|
| `NONE` (기본) | 듣기 링크 없음. 소개만 |
| `SEARCH_LINK` | 곡마다 유튜브 검색 결과 링크 |

- **서버가 결정한다.** 설정 `app.playback.strategy`에 따라 카탈로그의 곡마다 `listenUrl`을 주거나 주지 않고, 앱은 링크가 있을 때만 듣기 버튼을 보여준다. 재생 방식을 바꿔도 앱을 다시 배포하지 않아도 된다.
- 음원 파일로 만들거나 저장하는 방식은 유튜브 약관·저작권 문제로 **만들지 않는다.**

---

## 18. 가수 상세 → 앨범 → 곡별 좋아요/별로 `[구현됨]`

### 18.1 흐름
```
가수 카드 / 장르 상세의 핵심 가수 탭
  → 가수 상세: 계보에서의 역할(장르별 소개 + 대표곡), 정규 앨범 목록(커버)
  → 앨범 상세: 수록곡 목록, 곡마다 ♥ 좋아요 / ✕ 별로 (같은 버튼 다시 누르면 취소)
```

### 18.2 데이터 (참조 레이어)
| 엔티티 | 내용 |
|---|---|
| `Album` | 정규 앨범 = MusicBrainz release-group. 제목, 연도, 커버는 Cover Art Archive **링크로만** |
| `AlbumTrack` | 수록곡. 디스크 번호, 곡 번호, 제목, 길이 |
| `Artist.albumsSyncedAt` | 앨범 목록을 가져온 시각 (30일마다 갱신) |

### 18.3 가져오는 방식
| 시점 | MusicBrainz 호출 |
|---|---|
| 가수 상세를 처음 열 때 | 앨범 목록 1회 (100개씩 최대 3페이지). **보조 유형이 없는 정규 앨범만** 남긴다 (라이브·컴필레이션·데모 제외) |
| 앨범을 처음 열 때 | ① 공식 발매판 목록 → ② **가장 이른 발매판**의 수록곡. 연도만 있는 날짜(예: `2013`)는 그해 마지막 날로 보고 비교해서, 정확한 날짜가 있는 원래 발매판을 우선한다 |
| 두 번째부터 | 없음 (DB에서 바로) |

- 발매판 목록 조회에 수록곡을 포함하는 호출은 MusicBrainz가 허용하지 않아 두 번에 나눈다.
- 큐레이션 가수 110명의 MusicBrainz ID는 한 번만 실행한 스크립트로 찾아 시드 데이터에 넣었다 (107명 자동 확정, 3명 수동 확인: Alice Cooper = 밴드 시절, Grandmaster Flash = `&` 표기, Run-DMC = `Run‐D.M.C.` 표기).

### 18.4 곡별 평가
- 16장 선호 테이블을 그대로 쓴다. 대상 종류 `ALBUM_TRACK` 추가, `PreferenceHandler` 구현체 하나 추가.
- 선호 목록에는 서버가 **표시 이름**(`targetName`, 예: "Wonderwall · (What's the Story) Morning Glory?")을 함께 준다. 앱 카탈로그에 없는 대상이라서.

### 18.5 API
| Method | URL | 설명 |
|---|---|---|
| GET | /api/artists/{id} | 가수 상세 (처음 열 때 앨범 목록을 가져옴) |
| GET | /api/albums/{id} | 앨범 상세 + 곡별 내 선호 (처음 열 때 수록곡을 가져옴) |
| PUT | /api/me/preferences | `{targetType: "ALBUM_TRACK", targetId, preference}` |

---

## 19. 진행 방식: "다음 장르로", 되돌아보기, 직전 판정 바꾸기 `[구현됨]`

> 초기 설계(5.4)는 **스텝 완료 = 배정된 모든 곡에 반응**이었다. 판정 재료(반응 수)가 필요했기 때문인데,
> 소개 중심·재생 없음으로 바뀐 뒤에는 "듣지도 않은 곡에 반응해야 넘어갈 수 있는" 이상한 흐름이 되어 바꿨다.

### 19.1 "다음 장르로"
```
[다음 장르로 ›] → "○○, 어땠어요?"  (좋았어요 / 별로였어요 / 잘 모르겠어요)
                  └ 곡 반응을 남겼다면 그 다수결이 미리 선택됨
→ 판정 → (좋음·별로면) 경로 재계산 → 다음 스텝
```
- 곡 반응은 **선택**. 가수·곡 취향으로 쌓이고, 판정 질문의 기본값이 된다.
- **하루 제한은 없다.** 오늘 이 로드맵에서 넘긴 장르 수(`RoadmapStep.completedAt` 기준, `/api/today`의 `genresCompletedToday`)가 3개 이상이면 "오늘 벌써 N개 장르를 지나왔어요. 쉬어 가도 좋아요"라는 **막지 않는 안내**만 보여준다.

### 19.2 완료 방식 전략 `StepCompletionPolicy`
| 구현체 | 동작 |
|---|---|
| `MANUAL_NEXT` (기본) | "다음 장르로" + 판정 질문. 반응만으로는 넘어가지 않음 |
| `ALL_RATED` (초기 방식) | 배정된 곡에 모두 반응하면 다수결로 자동 판정. 버튼으로는 넘어갈 수 없음 |

설정 `app.step-completion.strategy`로 고른다.

### 19.3 ① 되돌아보기
- 오늘 화면의 **‹ 이전 장르**로 지나온 장르를 다시 본다.
- 지나온 장르의 곡에도 반응을 **추가·수정**할 수 있다. 같은 곡에 다시 보내면 새로 만들지 않고 고친다.
- 이미 끝난 **판정과 진행 위치는 바뀌지 않는다.** (반응은 취향 기록에만 쌓임)

### 19.4 ② 직전 판정 바꾸기 (좁은 범위)
- **마지막으로 판정한 장르만**, 그 뒤 장르에서 **아직 반응하지 않았을 때만** 바꿀 수 있다.
- 판정할 때마다 **판정 직전 상태를 스냅샷으로 저장**한다 (`VerdictSnapshot`: 판정한 스텝, 직전 로드맵 버전, 직전 장르 가중치). 로드맵마다 하나만 두고 덮어쓴다.
- 바꿀 때는 계산으로 되돌리지 않고(가중치 상한·하한 때문에 나눗셈으로 정확히 되돌릴 수 없음) **스냅샷으로 복원**한 뒤 새 판정을 다시 적용한다.
  - 재계산으로 새로 생긴 스텝(버전이 더 큰 스텝)은 지우고, 그때 빠졌던 스텝(`replacedAtVersion`)은 되살린다.
- 예: R&B에서 후손 찾기, 두왑을 "별로" → R&B → 두왑 → 로커빌리 → 브릿 인베이전 → 포크 록.
  "좋았어요"로 바꾸면 → R&B → 두왑 → 소울 → 펑크(Funk) → 디스코 (두왑의 이웃이 ×0.8이 되어 펑크가 사이키델릭보다 가까워짐). 두왑의 ×3 가중치는 남지 않는다.

### 19.5 API
| Method | URL | 설명 |
|---|---|---|
| POST | /api/roadmaps/current/next | `{verdict}` (없으면 곡 반응 다수결, 반응도 없으면 중립) |
| POST | /api/roadmaps/current/verdict-change | `{verdict}` 직전 판정 바꾸기 |
| GET | /api/today | `suggestedVerdict`, `canChangeLastVerdict`, `lastJudgedStepIndex`, `genresCompletedToday` 추가, `locked` 제거 |

| 에러 코드 | 상황 |
|---|---|
| STEP_COMPLETION_NOT_ALLOWED | `ALL_RATED` 설정에서 "다음 장르로"를 누름 |
| VERDICT_CHANGE_NOT_ALLOWED | 바꿀 수 있는 판정이 없음 (다음 장르에서 이미 반응 등) |

---

## 20. 데이터 구조: "코드 수정 없이 데이터만 추가" `[구현됨]`

> 새 장르나 가수를 넣을 때 탐색 로직·화면 코드는 고치지 않고 **데이터 파일만** 고친다.
> 탐험 전략은 "어떤 그래프가 주어지든" 부모·자식·형제를 따라가도록 만들어져 있어서, 코드 어디에도 특정 장르 이름이 없다.

### 20.1 파일 구성
```
MusicRoadmapServer/seed-data/        (서버 레포 안, 앱은 서버 레포를 옆에 받아 두고 참조)
├── main/seed/catalog.json          실제 데이터 (서버·앱이 같은 파일을 읽음)
└── fixture/seed/test-catalog.json  테스트 전용 고정 데이터 (록 28장르, 수정하지 않음)
```
- 서버: `build.gradle`의 `sourceSets`로 `seed-data/main`을 리소스에 포함. 설정 `app.seed.location`
- 앱: `app/build.gradle.kts`의 `sourceSets`로 `../../MusicRoadmapServer/seed-data`를 포함 (APK에는 실제 데이터만 들어감). 두 레포를 같은 폴더 아래에 나란히 받아야 빌드된다.
- 파일 내용: `genres`(코드·이름·시대·설명·계보도 좌표), `edges`(from→to), `tracks`(장르·곡·가수·연도·역할 소개), `aliases`(MusicBrainz 장르 별칭), `artistMbids`(가수 → MusicBrainz ID)
- **새 장르는 파일 뒤에 붙인다.** 장르 순서가 곧 빈 DB에서의 ID 순서라, 동률 처리(ID 작은 순)가 흔들리지 않게 하기 위함.

### 20.2 서버 반영: 없는 것만 추가
- 서버가 켜질 때마다 파일을 DB에 맞춘다(`SeedDataLoader.sync`). 빈 DB든 이미 쓰던 DB든 똑같이 동작한다.
- 없는 것만 추가: 장르(code), 간선(from→to), 가수(MusicBrainz ID → 없으면 큐레이션 가수 이름), 대표곡(장르·가수·제목), 장르-가수 소개, 별칭
- 이미 있는 큐레이션 장르의 설명·좌표와 소개 글은 파일 값으로 맞춘다 (파일이 원본).
- 유저 데이터(가져온 가수, 기록, 선호)와 파일에서 빠진 데이터는 **지우지 않는다.**
- 주의(실제로 겪은 버그): `run()`이 같은 클래스의 `sync()`를 부르면 `sync()`의 `@Transactional`이 적용되지 않는다(스프링 프록시를 거치지 않는 자기 호출). 트랜잭션이 없으면 저장 뒤에 바꾼 값(ID 연결)이 반영되지 않아 `run()`에도 트랜잭션을 걸었다.

### 20.3 테스트를 데이터에서 떼어 내기
- 탐색 알고리즘·서비스 테스트는 **고정 데이터(fixture)** 를 쓴다. 실제 데이터가 늘어나도 기대값(예: "브릿팝 가수 4명을 다 싫어하면 건너뜀")이 흔들리지 않는다.
- 실제 데이터에는 **규칙만 검사하는 테스트**(`SeedCatalogValidationTest`)를 둔다. 데이터를 추가해도 이 테스트는 고치지 않는다.
  - 장르 코드 중복 없음, 설명 있음 / 간선이 있는 장르만 잇고 중복·자기 자신·**순환 없음**
  - 장르마다 가수 4명 이상, 같은 장르에 같은 가수 중복 없음, 소개 300자 이하
  - 모든 가수에 MusicBrainz ID, ID 중복 없음 / 별칭 중복 없음 / **계보도 노드가 겹치지 않음**
- 이미 쓰던 DB에 새 데이터만 들어가는지, 두 번 실행해도 중복이 없는지는 `SeedDataLoaderTest`로 확인한다.

### 20.4 검증 결과 (Phase 3)
- 추가한 데이터: 재즈 계열 6장르(스윙, 비밥, 쿨 재즈, 하드 밥, 프리 재즈, 재즈 퓨전)와 간선 9개(스윙 → R&B로 록 계열과 연결), 기존 28장르에 가수 2명씩, 새 가수 78명의 MusicBrainz ID
  - 결과: 34장르, 52간선, 192곡, 188명
- 데이터 추가 전후로 소스 파일 176개의 해시를 비교해 **바뀐 파일은 `catalog.json` 하나**임을 확인했다.
- 코드 변경 없이 새 장르로 모든 탐험 방식이 동작했다. 예: 비밥 뿌리 찾기 → 스윙 → 재즈 → 블루스, 비밥 시대순 → 쿨 재즈 → 하드 밥 → 프리 재즈 → 재즈 퓨전, 재즈 퓨전 → 브릿팝 잇기 → 사이키델릭 록 → 글램 록 → 브릿팝
- 알게 된 점: 후손 찾기는 매 단계 한 갈래만 고르는 방식이라, 비밥처럼 자식이 여럿인데 고른 자식(쿨 재즈)이 막다른 장르면 1스텝으로 끝난다. → **막다른 곳이면 지나온 장르로 돌아가 다른 갈래로 이어 가도록 바꿨다** (5.2, `GreedyWalkStrategy.backtrackOnDeadEnd`, 서버·앱 동일). 뿌리 찾기는 "한 줄기 계보"가 의미라 그대로 둔다.
- 알게 된 점 2: 새 장르가 기존 별칭을 가져가야 할 때(예: `gangsta rap`이 힙합 → 지펑크) 예전 로더는 "없는 것만 추가"라 옮기지 못했다. → 별칭은 **파일이 가리키는 장르로 옮기도록** 바꿨다(`GenreAlias.pointTo`, 로그의 "옮김" 수). 데이터 파일 형식은 그대로다.

### 20.5 두 번째 검증: 일렉트로닉 · 힙합 세부 장르
- 위 두 코드 변경을 먼저 끝내고 해시를 찍은 뒤, **데이터만** 추가했다.
- 추가한 데이터: 11장르, 간선 18개, 가수 44명(장르마다 4명, 모두 MusicBrainz ID), 별칭 이동 5개
  - 일렉트로닉: 일렉트로닉, 신스팝, 하우스, 테크노, 드럼 앤 베이스, 트립합 (프로그레시브 록 → 일렉트로닉, 뉴 웨이브 → 신스팝, 디스코 → 하우스, 펑크 → 테크노로 기존 계보와 연결)
  - 힙합: 이스트코스트, 지펑크, 서던 힙합, 재즈 랩, 트랩 (하드 밥 → 재즈 랩으로 재즈 계열과 연결)
  - 별칭 이동: `synth-pop`·`synthpop` 뉴 웨이브 → 신스팝, `east coast hip hop` → 이스트코스트, `west coast hip hop`·`gangsta rap` → 지펑크
  - 결과: 45장르, 70간선, 236곡, 232명
- 소스 파일 176개 해시 비교: **바뀐 파일은 `catalog.json` 하나**. 서버 64개, 앱 24개 테스트 통과.
- 확인한 탐험 결과 (새 유저, maxSteps 6):
  - 힙합 후손 찾기: 드럼 앤 베이스 → 트립합 → 이스트코스트 → 지펑크 → 서던 힙합 → 트랩 (막다른 곳마다 힙합으로 돌아가 다음 갈래로, 마지막은 서던 힙합에서 트랩으로)
  - 일렉트로닉 후손 찾기: 신스팝 → 하우스 → 테크노 → 드럼 앤 베이스 → 트립합
  - 트랩 뿌리 찾기: 서던 힙합 → 힙합 → 펑크 → 두왑 → R&B → 블루스
  - 드럼 앤 베이스 → 비밥 잇기: 힙합 → 재즈 랩 → 하드 밥 → 비밥

