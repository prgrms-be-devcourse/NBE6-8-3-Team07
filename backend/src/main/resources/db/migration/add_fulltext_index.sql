-- Full-Text Search 인덱스 추가 및 대소문자 무시 설정
-- H2 Database에서 대소문자 구분 없는 검색을 위한 Collation 설정
--
-- 실행 방법:
-- 1. 개발 환경 (H2 Database):
--    - H2 Console에 접속: http://localhost:8080/h2-console
--    - 또는 IDE의 Database 도구에서 H2 데이터베이스 연결
--    - 이 SQL 파일 전체를 복사하여 실행
--
-- 2. 배포 환경 (MySQL/PostgreSQL):
--    - MySQL: mysql -u [username] -p [database_name] < add_fulltext_index.sql
--    - PostgreSQL: psql -U [username] -d [database_name] -f add_fulltext_index.sql
--    - 또는 배포 서버의 데이터베이스 관리 도구에서 실행
--
-- 주의사항:
-- - 데이터베이스 백업 후 실행 권장
-- - 대용량 테이블의 경우 인덱스 생성 시간이 오래 걸릴 수 있음

-- 기존 인덱스 제거 (있을 경우)
DROP INDEX IF EXISTS idx_fairytale_title_content;
DROP INDEX IF EXISTS idx_fairytale_title;
DROP INDEX IF EXISTS idx_fairytale_content;

-- 컬럼을 대소문자 무시 Collation으로 변경
ALTER TABLE fairytale ALTER COLUMN title SET DATA TYPE VARCHAR(500) COLLATE ENGLISH_UNITED_STATES_CI;
ALTER TABLE fairytale ALTER COLUMN content SET DATA TYPE TEXT COLLATE ENGLISH_UNITED_STATES_CI;

-- 대소문자 무시 설정된 컬럼에 인덱스 추가
CREATE INDEX idx_fairytale_title ON fairytale(title);
CREATE INDEX idx_fairytale_content ON fairytale(content);
CREATE INDEX idx_fairytale_title_content ON fairytale(title, content);