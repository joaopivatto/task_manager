-- MariaDB bootstrap script for TaskFlow
-- Usage example:
--   mariadb -u root -p < src/main/resources/db/mariadb/create_taskdb.sql

CREATE DATABASE IF NOT EXISTS taskdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE taskdb;

CREATE TABLE IF NOT EXISTS category (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_category_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  status ENUM('TO_DO', 'DOING', 'DONE', 'STUCK') NOT NULL DEFAULT 'TO_DO',
  deadline DATE NULL,
  completed_at DATETIME(6) NULL,
  archived BIT(1) NOT NULL DEFAULT b'0',
  category_id BIGINT NULL,
  PRIMARY KEY (id),
  KEY idx_task_status_archived (status, archived),
  KEY idx_task_completed_at (completed_at),
  CONSTRAINT fk_task_category
    FOREIGN KEY (category_id)
    REFERENCES category (id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS task_update (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  comment VARCHAR(2000) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_task_update_task_created (task_id, created_at),
  CONSTRAINT fk_task_update_task
    FOREIGN KEY (task_id)
    REFERENCES task (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

