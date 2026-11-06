# EnjoyTrip 개발 문서

`temp/backend-requirements.pdf`의 요구사항과 이번 프로젝트에서 확정한 구현 범위를 정리한 문서입니다. `temp/`는 Git에서 제외합니다.

| 문서 | 내용 |
| --- | --- |
| [01-requirements.md](01-requirements.md) | 요구사항, 구현 범위, 기능별 완료 조건 |
| [02-architecture.md](02-architecture.md) | Servlet/JSP 구조, 화면·요청 경로, 데이터 모델, 외부 연동, 다이어그램 |
| [03-implementation-phases.md](03-implementation-phases.md) | 선행 조건과 단계별 구현 순서·완료 기준 |
| [04-test-plan.md](04-test-plan.md) | 기능·권한·연동·화면 검증 시나리오 |
| [05-implementation-status.md](05-implementation-status.md) | 구현 완료 항목과 실제 검증 결과·남은 외부 연동 확인 |
| [06-local-run.md](06-local-run.md) | Windows 로컬 실행, DB 설정, 오류 해결 |

## 확정된 방향

- 명세서의 필수·선택·심화 및 부가 기능을 모두 구현합니다.
- **Servlet(Controller) → Service → Dao → Dto** 구조를 필수로 지킵니다. JSP는 화면 렌더링에 사용합니다.
- 관광 데이터는 공공 API에서 수집해 MySQL에 저장하고, 사용자 조회는 DB에서 처리합니다.
- 비밀번호 찾기는 관리자 초기화 방식입니다. 공지사항은 관리자만, 공유게시판은 로그인 회원이 작성하고 본인 글만 수정·삭제합니다.
- 뉴스는 공개 RSS의 제목·발행일·출처·원문 링크를 수집합니다.

## 구현 전 준비

공공데이터포털 서비스 키, OpenWeather API 키, MySQL 8 실행 환경이 필요합니다. 지도는 Leaflet과 OpenStreetMap을 사용해 별도 키가 필요하지 않습니다. 키와 DB 비밀번호는 저장소에 커밋하지 않습니다. 키가 없는 환경에서도 외부 연동을 대체한 테스트를 실행할 수 있도록 설계합니다.
