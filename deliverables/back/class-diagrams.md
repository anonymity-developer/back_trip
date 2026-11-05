# 개선된 클래스 다이어그램

현재 `src/main/java/kr/co/enjoytrip`의 실제 클래스와 의존성을 기준으로 작성했다. 큰 그림을 한 장에 압축하지 않고 관광지·콘텐츠·회원/운영 세 영역으로 분리했다. 화살표는 **호출하거나 보유하는 방향**이다.

## 1. 관광지 검색과 외부 정보

```mermaid
classDiagram
direction LR
class AttractionController {
  -AttractionService attractions
  +doGet(req, res)
  +doPost(req, res)
}
class AttractionSearchController {
  -AttractionDao dao
  +doGet(req, res)
}
class AttractionService {
  -AttractionDao dao
  -TourApiClient api
  +search(area, type, keyword, page)
  +find(id)
  +images(attraction)
  +syncAll()
}
class AttractionDao {
  +search(area, type, keyword, page, size)
  +find(id)
  +upsertAll(rows)
  +images(id)
}
class AttractionDto {
  +long id
  +long contentId
  +String title
  +int contentType
  +Double latitude
  +Double longitude
}
class TourApiClient {
  +fetchPage(area, page, size)
  +fetchOverview(contentId)
  +fetchImages(contentId)
}
class InfoController {
  -AttractionDao attractions
  -InfoService info
  +doGet(req, res)
}
class InfoService {
  -XmlHttpClient xml
  +weather(attraction)
  +sun(attraction, day)
  +chargers(attraction)
}
class XmlHttpClient {
  +get(url)
}
AttractionController --> AttractionService
AttractionSearchController --> AttractionDao
AttractionService --> AttractionDao
AttractionService --> TourApiClient
AttractionDao --> AttractionDto
TourApiClient --> AttractionDto
InfoController --> AttractionDao
InfoController --> InfoService
InfoService --> AttractionDto
InfoService --> XmlHttpClient
```

`AttractionSearchController`와 `InfoController`는 각각 `AttractionDao`를 직접 사용한다. 이 예외를 숨기지 않고 실제 구현대로 표시했다. JSP는 Controller가 선택하는 뷰이며, 지도는 브라우저의 `maps.js`와 Leaflet에서 렌더링한다.

[관광지 클래스 다이어그램 SVG](diagrams/classes-attractions.svg)

## 2. 여행 계획, Hot Place, 글

```mermaid
classDiagram
direction LR
class PlanController {
  -PlanService plans
  +doGet(req, res)
  +doPost(req, res)
}
class PlanService {
  -PlanDao dao
  -AttractionDao attractions
  +visible(id, viewer)
  +owned(id, viewer)
  +create(...)
  +addStop(...)
  +reorder(...)
}
class PlanDao {
  +list(viewer)
  +find(id)
  +stops(planId)
  +insert(...)
  +addStop(...)
  +reorder(planId, ids)
}
class PlanDto {
  +long id
  +long userId
  +boolean isPublic
  +LocalDate startDate
  +LocalDate endDate
}
class PlanStopDto {
  +long id
  +long planId
  +long attractionId
  +int order
  +Double latitude
  +Double longitude
}
class HotPlaceController {
  -HotPlaceService places
  +doGet(req, res)
  +doPost(req, res)
}
class PhotoController {
  -HotPlaceService places
  +doGet(req, res)
}
class HotPlaceService {
  -HotPlaceDao dao
  +create(...)
  +update(...)
  +delete(id, user)
  +photoPath(place)
}
class HotPlaceDao {
  +list()
  +find(id)
  +insert(...)
  +update(...)
}
class HotPlaceDto {
  +long id
  +long userId
  +String imagePath
  +Double latitude
  +Double longitude
}
class PostController {
  -PostService posts
  +doGet(req, res)
  +doPost(req, res)
}
class PostService {
  -PostDao dao
  +create(...)
  +update(...)
  +delete(...)
}
class PostDao {
  +list(kind, keyword, page)
  +find(id, kind)
  +insert(...)
}
class PostDto {
  +long id
  +long userId
  +String kind
  +String title
}
PlanController --> PlanService
PlanService --> PlanDao
PlanService --> AttractionDao
PlanDao --> PlanDto
PlanDao --> PlanStopDto
HotPlaceController --> HotPlaceService
PhotoController --> HotPlaceService
HotPlaceService --> HotPlaceDao
HotPlaceDao --> HotPlaceDto
PostController --> PostService
PostService --> PostDao
PostDao --> PostDto
```

`PlanService.visible`은 공개 여부와 소유자를 검사하고 `owned`는 수정 권한을 검사한다. `HotPlaceService`가 사진 파일 형식·크기와 저장 경로를 관리한다. 공지와 게시판은 `PostDto.kind`로 구분하고 `PostService`가 관리자/작성자 권한을 검사한다.

[콘텐츠 클래스 다이어그램 SVG](diagrams/classes-content.svg)

## 3. 회원, 뉴스, 공통 운영

```mermaid
classDiagram
direction LR
class AuthController {
  -UserService users
  +doGet(req, res)
  +doPost(req, res)
}
class UserService {
  -UserDao dao
  +register(...)
  +authenticate(login, password)
  +delete(id, password)
  +requestReset(email)
  +completeReset(requestId, adminId)
}
class UserDao {
  +findByLogin(login)
  +findById(id)
  +insert(...)
  +pendingResets()
}
class UserDto {
  +long id
  +String loginId
  +String role
  +boolean mustChangePassword
}
class PasswordHasher {
  +hash(password)
  +verify(password, encoded)
  +temporaryPassword()
}
class NewsController {
  -NewsService news
  +doGet(req, res)
  +doPost(req, res)
}
class NewsService {
  -NewsDao dao
  -XmlHttpClient http
  +list(page)
  +sync()
}
class NewsDao {
  +list(page)
  +insert(...)
}
class NewsDto {
  +long id
  +String title
  +String source
  +String url
}
class AppLifecycle {
  +contextInitialized(event)
  +contextDestroyed(event)
  +startTourSync()
  +startNewsSync()
}
class SecurityFilter {
  +doFilter(request, response, chain)
}
class Database {
  +open()
}
AuthController --> UserService
UserService --> UserDao
UserService --> PasswordHasher
UserService --> HotPlaceDao
UserService --> HotPlaceService
UserDao --> UserDto
NewsController --> NewsService
NewsService --> NewsDao
NewsService --> XmlHttpClient
NewsDao --> NewsDto
AppLifecycle --> AttractionService
AppLifecycle --> NewsService
AppLifecycle --> UserDao
UserDao --> Database
NewsDao --> Database
```

`SecurityFilter`는 모든 요청 앞에서 세션·CSRF·관리자 경로 접근을 검사한다. DAO들은 공통 `Database`를 통해 JDBC 연결을 얻는다. `AppLifecycle`은 관리자 초기 계정 생성과 수집 스케줄러를 시작한다. DTO는 Java `record`로 구현했다.

[회원·뉴스·운영 클래스 다이어그램 SVG](diagrams/classes-auth-news.svg)

## 계층 간 요청 흐름

```mermaid
sequenceDiagram
  actor Browser as 브라우저
  participant Filter as SecurityFilter
  participant Controller as Servlet Controller
  participant Service as Service
  participant Dao as Dao
  participant DB as MySQL
  participant JSP as JSP
  Browser->>Filter: HTTP 요청
  Filter->>Controller: 인증·CSRF 확인 후 전달
  Controller->>Service: 업무 처리
  Service->>Dao: 조회/저장
  Dao->>DB: PreparedStatement
  DB-->>Dao: 결과
  Dao-->>Service: DTO
  Service-->>Controller: 처리 결과
  Controller->>JSP: 데이터 설정 및 forward
  JSP-->>Browser: HTML 응답
```

[요청 흐름 시퀀스 SVG](diagrams/request-sequence.svg)
