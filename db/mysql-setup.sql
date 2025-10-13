-- MySQL setup for Step 3 tests (Hybrid Server)
-- Creates database, user and required table.

-- Run as a privileged user (e.g., root):
--   mysql -u root -p < db/mysql-setup.sql

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `hstestdb`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'hsdb'@'localhost' IDENTIFIED BY 'hsdbpass';
GRANT ALL PRIVILEGES ON `hstestdb`.* TO 'hsdb'@'localhost';
FLUSH PRIVILEGES;

USE `hstestdb`;

CREATE TABLE IF NOT EXISTS `HTML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Quick sanity checks
SHOW DATABASES LIKE 'hstestdb';
SHOW TABLES;
