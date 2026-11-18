# be16261118report

글(Post)과 댓글(PostComment) CRUD를 제공하는 Spring Boot REST API입니다.

- Java 25, Spring Boot 4.1.2, Spring Data JPA, AspectJ, Bean Validation
- 레이어드 구조
  - 글: `PostController` → `PostService` → `PostRepository`
  - 댓글: `PostCommentController`만 존재. 글 엔티티의 `addComment` / `modifyComment` / `removeComment`를 호출하면
    트랜잭션 안에서 더티체킹(cascade, orphanRemoval)으로 DB에 반영됩니다.
- 모든 컨트롤러·서비스 메서드는 트랜잭션 안에서 동작합니다.

## 실행 환경

| 프로필 | DB | 설명 |
|---|---|---|
| `test` | H2 인메모리 | 테스트 전용 (`application-test.yaml`) |
| `dev` | MySQL | 기본 프로필. 접속 정보는 프로젝트 루트의 `.env`에서 읽음 (`application-dev.yaml`) |

### `.env` 설정 (dev)

`.env.example`을 복사해 값을 채웁니다. `.env`는 git에 올라가지 않습니다.

```
DB_HOST=
DB_PORT=
DB_NAME=
DB_USERNAME=
DB_PASSWORD=
```

### 실행 / 테스트

```bash
./gradlew bootRun   # dev 프로필로 실행 (http://localhost:8080)
./gradlew test      # test 프로필(H2 인메모리)로 전체 테스트
```

## API 목록

### 글

| 메서드 | 경로 | 요청 본문 | 성공 응답 |
|---|---|---|---|
| GET | `/api/v1/posts` | - | 200 `PostDto[]` (최신순) |
| GET | `/api/v1/posts/{id}` | - | 200 `PostDto` |
| POST | `/api/v1/posts` | `{"title", "content"}` | 201 RsData `201-1` "N번 글이 작성되었습니다." + `PostDto` |
| PUT | `/api/v1/posts/{id}` | `{"title", "content"}` | 200 RsData `200-1` "N번 글이 수정되었습니다." + `PostDto` |
| DELETE | `/api/v1/posts/{id}` | - | 200 RsData `200-1` "N번 글이 삭제되었습니다." (댓글도 함께 삭제) |

### 댓글

| 메서드 | 경로 | 요청 본문 | 성공 응답 |
|---|---|---|---|
| GET | `/api/v1/posts/{postId}/comments` | - | 200 `PostCommentDto[]` (작성순) |
| GET | `/api/v1/posts/{postId}/comments/{id}` | - | 200 `PostCommentDto` |
| POST | `/api/v1/posts/{postId}/comments` | `{"content"}` | 201 RsData `201-1` "N번 댓글이 작성되었습니다." + `PostCommentDto` |
| PUT | `/api/v1/posts/{postId}/comments/{id}` | `{"content"}` | 200 RsData `200-1` "N번 댓글이 수정되었습니다." + `PostCommentDto` |
| DELETE | `/api/v1/posts/{postId}/comments/{id}` | - | 200 RsData `200-1` "N번 댓글이 삭제되었습니다." |

요청 본문의 모든 필드는 `@NotBlank` 검증을 거칩니다.

## 응답 형식

조회 API는 DTO를 그대로 반환하고, 작성/수정/삭제와 모든 오류 응답은 `RsData`로 감싸 반환합니다.

**PostDto**

```json
{ "id": 1, "createDate": "2026-11-18T15:21:13.551449", "modifyDate": "2026-11-18T15:21:13.551449", "title": "제목", "content": "내용" }
```

**PostCommentDto**

```json
{ "id": 1, "createDate": "2026-11-18T15:21:13.903159", "modifyDate": "2026-11-18T15:21:13.903159", "postId": 1, "content": "댓글" }
```

**RsData**

```json
{ "resultCode": "201-1", "msg": "1번 글이 작성되었습니다.", "data": { } }
```

`resultCode`는 `HTTP상태코드-세부코드` 형식입니다. `RsDataAspect`가 RsData를 반환하는
`@RestController` / `@RestControllerAdvice` 메서드의 실행 후, `-` 앞 숫자를 HTTP 응답 상태 코드로 지정합니다.
(예: `404-1` → 404)

## 오류 응답

`GlobalExceptionHandler`가 모든 예외를 RsData로 감싸 반환합니다.

| resultCode | 상황 |
|---|---|
| `400-1` | 요청 본문 검증 실패 (msg: `필드-검증코드-메시지`, 줄바꿈으로 구분) |
| `400-2` | 요청 본문 JSON 형식 오류 |
| `400-3` | 경로 변수 타입 불일치 (예: `/api/v1/posts/abc`) |
| `404-1` | 없는 글/댓글, 또는 다른 글의 댓글 |
| `404-2` | 존재하지 않는 경로 |
| `405-1` | 지원하지 않는 HTTP 메서드 |
| `500-1` | 그 외 서버 오류 |
| 지정값 | `ServiceException(resultCode, msg)`을 던질 때 지정한 코드 |
