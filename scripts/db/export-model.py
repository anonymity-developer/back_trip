"""Export this project's DDL columns as a Mermaid ERD and data dictionary."""
from pathlib import Path
import re
import shutil


def main():
    root = Path(__file__).resolve().parents[2]
    source = root / 'src/main/resources/schema.sql'
    destination = root / 'deliverables/db'
    schema = source.read_text(encoding='utf-8')
    tables = re.findall(r'^CREATE TABLE IF NOT EXISTS (\w+) \((.*?)^\)(?: ENGINE=InnoDB)?;', schema, re.S | re.M)
    if len(tables) != 12:
        raise ValueError('Expected 12 tables; review export parser/model after schema changes.')
    relations = '''    sidos ||--o{ guguns : sido_code
    sidos ||--o{ attractions : area_code
    guguns o|--o{ attractions : area_sigungu
    contenttypes ||--o{ attractions : content_type
    attractions ||--o{ attraction_images : CASCADE
    users ||--o{ plans : CASCADE
    plans ||--o{ plan_stops : CASCADE
    attractions ||--o{ plan_stops : RESTRICT
    users ||--o{ hot_places : CASCADE
    users ||--o{ posts : CASCADE
    users ||--o{ password_reset_requests : user_id_CASCADE
    users o|--o{ password_reset_requests : processed_by_SET_NULL
'''
    mermaid = 'erDiagram\n' + relations
    markdown = '''# 전체 ERD와 데이터 정의

실제 `src/main/resources/schema.sql`에서 추출한 12개 테이블의 모델이다. 관계선과 복합 제약은 아래 설명을 함께 확인한다.

[ERD SVG](erd.svg) · [Mermaid 편집본](erd.mmd) · [최종 DDL](schema.sql)

'''
    for name, body in tables:
        foreign = {column.strip() for group in re.findall(r'FOREIGN KEY \(([^)]+)\)', body) for column in group.split(',')}
        mermaid += f'    {name} {{\n'
        markdown += f'## {name}\n\n| 컬럼 | 실제 DDL 정의 |\n| --- | --- |\n'
        seen = set()
        for line in body.splitlines():
            line = line.strip().rstrip(',')
            match = re.match(r'(\w+)\s+(BIGINT|INT|VARCHAR|TEXT|DECIMAL|ENUM|BOOLEAN|TIMESTAMP|DATETIME|DATE)\b', line)
            if not match:
                continue
            column, data_type = match.groups()
            if column in seen:
                raise ValueError(f'Duplicate parsed column: {name}.{column}')
            seen.add(column)
            flags = []
            if 'PRIMARY KEY' in line:
                flags.append('PK')
            if column in foreign:
                flags.append('FK')
            if 'UNIQUE' in line:
                flags.append('UK')
            mermaid += f'        {data_type} {column}' + (' ' + ','.join(flags) if flags else '') + '\n'
            markdown += f'| `{column}` | `{line[len(column):].strip()}` |\n'
        mermaid += '    }\n'
    markdown += '''
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
'''
    destination.mkdir(parents=True, exist_ok=True)
    (destination / 'erd.mmd').write_text(mermaid, encoding='utf-8')
    (destination / 'erd.md').write_text(markdown, encoding='utf-8')
    shutil.copyfile(source, destination / 'schema.sql')
    print('Exported 12 tables; DDL copy matches source. Render erd.mmd to erd.svg separately.')


if __name__ == '__main__':
    main()
