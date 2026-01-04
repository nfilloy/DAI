-- MySQL setup for Hybrid Server Tests
-- Creates all databases, user and required tables for all test stages.
-- Includes: hstestdb (main), hstestdb1-4 (for Step7 multiple servers tests)

-- Run as a privileged user (e.g., root):
--   mysql -u root -p < db/mysql-setup.sql

SET NAMES utf8mb4;

-- ============================================================
-- Create user first
-- ============================================================
CREATE USER IF NOT EXISTS 'hsdb'@'localhost' IDENTIFIED BY 'hsdbpass';

-- ============================================================
-- Database: hstestdb (main - Steps 3, 4, 6)
-- ============================================================
CREATE DATABASE IF NOT EXISTS `hstestdb`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON `hstestdb`.* TO 'hsdb'@'localhost';

USE `hstestdb`;

CREATE TABLE IF NOT EXISTS `HTML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSD` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSLT` (
  `uuid` CHAR(36) NOT NULL,
  `xsd` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- Database: hstestdb1 (Step7 - Server 1)
-- ============================================================
CREATE DATABASE IF NOT EXISTS `hstestdb1`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON `hstestdb1`.* TO 'hsdb'@'localhost';

USE `hstestdb1`;

CREATE TABLE IF NOT EXISTS `HTML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSD` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSLT` (
  `uuid` CHAR(36) NOT NULL,
  `xsd` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- Database: hstestdb2 (Step7 - Server 2)
-- ============================================================
CREATE DATABASE IF NOT EXISTS `hstestdb2`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON `hstestdb2`.* TO 'hsdb'@'localhost';

USE `hstestdb2`;

CREATE TABLE IF NOT EXISTS `HTML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSD` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSLT` (
  `uuid` CHAR(36) NOT NULL,
  `xsd` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- Database: hstestdb3 (Step7 - Server 3)
-- ============================================================
CREATE DATABASE IF NOT EXISTS `hstestdb3`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON `hstestdb3`.* TO 'hsdb'@'localhost';

USE `hstestdb3`;

CREATE TABLE IF NOT EXISTS `HTML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSD` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSLT` (
  `uuid` CHAR(36) NOT NULL,
  `xsd` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- Database: hstestdb4 (Step7 - Server 4)
-- ============================================================
CREATE DATABASE IF NOT EXISTS `hstestdb4`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON `hstestdb4`.* TO 'hsdb'@'localhost';

USE `hstestdb4`;

CREATE TABLE IF NOT EXISTS `HTML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XML` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSD` (
  `uuid` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `XSLT` (
  `uuid` CHAR(36) NOT NULL,
  `xsd` CHAR(36) NOT NULL,
  `content` TEXT NOT NULL,
  PRIMARY KEY (`uuid`)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- Apply privileges
-- ============================================================
FLUSH PRIVILEGES;

-- ============================================================
-- Quick sanity checks
-- ============================================================
SHOW DATABASES LIKE 'hstestdb%';
SELECT 'Setup complete! Created databases: hstestdb, hstestdb1, hstestdb2, hstestdb3, hstestdb4' AS Status;
