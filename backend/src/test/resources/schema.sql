-- 테스트 환경용 스키마 및 인덱스 설정
-- H2 인메모리 DB에서 LIKE 쿼리 성능 최적화를 위한 B-Tree 인덱스
-- 주의: H2는 Full-Text Search를 완전 지원하지 않으므로 일반 인덱스 사용

-- 기존 인덱스 제거 (오류 무시)
DROP INDEX IF EXISTS idx_fairytale_title_content;
DROP INDEX IF EXISTS idx_fairytale_title;
DROP INDEX IF EXISTS idx_fairytale_content;
DROP INDEX IF EXISTS idx_fairytale_created_at;
DROP INDEX IF EXISTS idx_fairytale_title_created_at;
DROP INDEX IF EXISTS idx_fairytale_content_created_at;

-- LIKE '%keyword%' 쿼리 성능 개선을 위한 B-Tree 인덱스
-- Full-Text가 아니지만 일부 성능 개선 효과 있음
CREATE INDEX IF NOT EXISTS idx_fairytale_title ON fairytale(title);
CREATE INDEX IF NOT EXISTS idx_fairytale_content ON fairytale(content);
CREATE INDEX IF NOT EXISTS idx_fairytale_created_at ON fairytale(created_at DESC);

-- 복합 인덱스로 검색 + 정렬 동시 최적화  
CREATE INDEX IF NOT EXISTS idx_fairytale_title_created_at ON fairytale(title, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fairytale_content_created_at ON fairytale(content, created_at DESC);

-- H2 성능 튜닝 설정 (최신 H2 버전에서는 자동 최적화)