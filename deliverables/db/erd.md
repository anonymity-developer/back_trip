# 전체 ERD와 데이터 정의

필수 제출용 편집 모델은 [EnjoyTrip_db.mwb](EnjoyTrip_db.mwb), 같은 모델의 이미지 파일은 [EnjoyTrip_db.png](EnjoyTrip_db.png), 실행용 전체 SQL은 [EnjoyTrip_db.sql](EnjoyTrip_db.sql)이다. 아래 Mermaid·컬럼 정의는 보조 설명이다.

실제 `src/main/resources/schema.sql`에서 추출한 12개 테이블의 모델이다. 관계선과 복합 제약은 아래 설명을 함께 확인한다.

[ERD SVG](erd.svg) · [Mermaid 편집본](erd.mmd) · [최종 DDL](schema.sql)

## users

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `id` | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| `login_id` | `VARCHAR(50) NOT NULL UNIQUE` |
| `email` | `VARCHAR(255) NOT NULL UNIQUE` |
| `name` | `VARCHAR(80) NOT NULL` |
| `password_hash` | `VARCHAR(255) NOT NULL` |
| `role` | `ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER'` |
| `must_change_password` | `BOOLEAN NOT NULL DEFAULT FALSE` |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` |
## sidos

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `no` | `INT PRIMARY KEY AUTO_INCREMENT` |
| `sido_code` | `INT NOT NULL UNIQUE` |
| `sido_name` | `VARCHAR(20)` |
## guguns

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `no` | `INT PRIMARY KEY AUTO_INCREMENT` |
| `sido_code` | `INT NOT NULL` |
| `gugun_code` | `INT NOT NULL` |
| `gugun_name` | `VARCHAR(20)` |
## contenttypes

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `content_type_id` | `INT PRIMARY KEY` |
| `content_type_name` | `VARCHAR(45)` |
## attractions

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `id` | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| `content_id` | `BIGINT NOT NULL UNIQUE` |
| `title` | `VARCHAR(255) NOT NULL` |
| `content_type` | `INT NOT NULL` |
| `area_code` | `INT NOT NULL` |
| `sigungu_code` | `INT` |
| `address` | `VARCHAR(500)` |
| `description` | `TEXT` |
| `latitude` | `DECIMAL(11,8)` |
| `longitude` | `DECIMAL(11,8)` |
| `image_url` | `VARCHAR(1000)` |
| `source_updated_at` | `VARCHAR(30)` |
| `synced_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` |
## attraction_images

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `id` | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| `attraction_id` | `BIGINT NOT NULL` |
| `image_url` | `VARCHAR(1000) NOT NULL` |
| `credit` | `VARCHAR(255)` |
| `display_order` | `INT NOT NULL DEFAULT 0` |
## plans

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `id` | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| `user_id` | `BIGINT NOT NULL` |
| `title` | `VARCHAR(200) NOT NULL` |
| `start_date` | `DATE NOT NULL` |
| `end_date` | `DATE NOT NULL` |
| `budget` | `DECIMAL(12,2) NOT NULL DEFAULT 0` |
| `details` | `TEXT` |
| `is_public` | `BOOLEAN NOT NULL DEFAULT FALSE` |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` |
| `updated_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` |
## plan_stops

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `id` | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| `plan_id` | `BIGINT NOT NULL` |
| `attraction_id` | `BIGINT NOT NULL` |
| `visit_date` | `DATE NOT NULL` |
| `stop_order` | `INT NOT NULL` |
| `budget` | `DECIMAL(12,2) NOT NULL DEFAULT 0` |
| `memo` | `VARCHAR(1000)` |
## hot_places

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `id` | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| `user_id` | `BIGINT NOT NULL` |
| `title` | `VARCHAR(200) NOT NULL` |
| `place_name` | `VARCHAR(200) NOT NULL` |
| `place_type` | `VARCHAR(80) NOT NULL` |
| `visited_on` | `DATE NOT NULL` |
| `description` | `TEXT NOT NULL` |
| `latitude` | `DECIMAL(11,8)` |
| `longitude` | `DECIMAL(11,8)` |
| `image_path` | `VARCHAR(255)` |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` |
| `updated_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` |
## news

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `id` | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| `title` | `VARCHAR(500) NOT NULL` |
| `source` | `VARCHAR(100) NOT NULL` |
| `source_url` | `VARCHAR(1000) NOT NULL` |
| `published_at` | `DATETIME` |
| `collected_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` |
## posts

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `id` | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| `user_id` | `BIGINT NOT NULL` |
| `kind` | `ENUM('NOTICE','BOARD') NOT NULL` |
| `title` | `VARCHAR(200) NOT NULL` |
| `body` | `TEXT NOT NULL` |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` |
| `updated_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` |
## password_reset_requests

| 컬럼 | 실제 DDL 정의 |
| --- | --- |
| `id` | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| `user_id` | `BIGINT NOT NULL` |
| `status` | `ENUM('PENDING','COMPLETED') NOT NULL DEFAULT 'PENDING'` |
| `requested_at` | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` |
| `processed_at` | `TIMESTAMP NULL` |
| `processed_by` | `BIGINT NULL` |

## 복합 제약과 삭제 정책

- `guguns(sido_code,gugun_code)`는 복합 UNIQUE다. 구군 코드 단독은 전역 고유하지 않다.
- `attractions(area_code,sigungu_code)`는 위 복합키를 참조한다. 구군은 NULL 허용, 시도는 필수다. 시도·유형은 각각 별도 FK도 갖는다.
- 공공 원본 ID인 `content_id`는 UNIQUE이며 앱 내부 `id`와 다르다. 원본 덤프 `no`로 앱 id를 대체하지 않는다.
- `plan_stops(plan_id,visit_date,stop_order)`는 복합 UNIQUE다. 관광지 참조는 삭제 제한, 계획 삭제는 방문지 CASCADE다.
- 사진·뉴스의 고유 인덱스는 `(attraction_id,image_url(255))`, `source_url(255)`인 접두 인덱스다. URL 전체 고유 제약과 구분한다.
- 비밀번호 초기화 요청에는 요청자(user_id)와 처리자(processed_by) 두 관계가 있다. 요청자 삭제는 CASCADE, 처리자 삭제는 SET NULL이다.
- 기준정보 FK는 기본 삭제·수정 제한이다. 사용자 데이터의 기존 CASCADE를 유지한다.
- 그림은 타입 이름과 키를 요약한다. 길이·정밀도·NULL·기본값의 정확한 기준은 데이터 정의표와 최종 DDL이다.

신규 설치는 DDL 후 기준정보 seed를 실행한다. 기존 DB에는 별도 업그레이드 SQL을 적용한다. [실행 안내](../../docs/db/06-local-test.md)를 참고한다.
