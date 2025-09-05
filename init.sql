-- 초기 데이터베이스 설정
CREATE DATABASE IF NOT EXISTS liteware CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE liteware;

-- 권한 설정
GRANT ALL PRIVILEGES ON liteware.* TO 'liteware'@'%';
FLUSH PRIVILEGES;