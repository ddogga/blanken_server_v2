-- 카테고리 초기 데이터.
-- name 이 UNIQUE 라 ON CONFLICT DO NOTHING 으로 여러 번 실행해도 안전하다.
-- ddl-auto: create 로 매번 테이블이 초기화되는 개발 환경 기준.
--
-- 수동 실행:  psql -U user -d blanken -f src/main/resources/data.sql
-- 기동 시 자동 실행:  application.yaml 에 아래 두 줄 추가
--   spring.sql.init.mode: always
--   spring.jpa.defer-datasource-initialization: true   (Hibernate 가 테이블을 만든 뒤 실행되도록)

INSERT INTO category (name) VALUES
    ('토익'),
    ('토플'),
    ('수능'),
    ('비즈니스'),
    ('일상회화'),
    ('여행'),
    ('문법'),
    ('숙어'),
    ('동사'),
    ('시사')
ON CONFLICT (name) DO NOTHING;
