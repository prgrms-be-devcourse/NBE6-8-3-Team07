-- Full-Text Search 인덱스 추가 및 대소문자 무시 설정
-- H2 Database에서 대소문자 구분 없는 검색을 위한 Collation 설정

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