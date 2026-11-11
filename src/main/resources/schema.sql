-- ================================
-- GITHUB 관련
-- ================================

CREATE TABLE IF NOT EXISTS `GITHUB_USERS` (
  `GITHUB_ID`         bigint       PRIMARY KEY,
  `USERNAME`          varchar(255) UNIQUE NOT NULL,
  `PROFILE_IMAGE_URL` varchar(255),
  `ACCESS_TOKEN`      text         NOT NULL,
  `REFRESH_TOKEN`     text,
  `TOKEN_EXPIRES_AT`  datetime,
  `CREATED_AT`        datetime,
  `UPDATED_AT`        datetime
);

CREATE TABLE IF NOT EXISTS `REPOSITORIES` (
  `REPOSITORY_ID`   bigint       PRIMARY KEY,
  `GITHUB_ID`       bigint       NOT NULL,
  `OWNER_NAME`      varchar(255) NOT NULL,
  `REPOSITORY_NAME` varchar(255) NOT NULL,
  `DEFAULT_BRANCH`  varchar(255) NOT NULL,
  `IS_PRIVATE`      boolean      NOT NULL,
  `LAST_SYNCED_AT`  datetime,
  `CREATED_AT`      datetime,
  `UPDATED_AT`      datetime,

  UNIQUE KEY `REPOSITORIES_index_0` (`GITHUB_ID`, `OWNER_NAME`, `REPOSITORY_NAME`),
  CONSTRAINT `FK_REPOSITORIES_GITHUB_USER`
    FOREIGN KEY (`GITHUB_ID`) REFERENCES `GITHUB_USERS` (`GITHUB_ID`)
);


-- ================================
-- 마스터 데이터
-- ================================

CREATE TABLE IF NOT EXISTS `WCAG_ITEMS` (
  `WCAG_ITEM_ID` bigint       PRIMARY KEY,
  `SC`           varchar(20)  NOT NULL COMMENT '예: 1.1.1',
  `TITLE`        varchar(255) NOT NULL,
  `LEVEL_TYPE`   varchar(5)   NOT NULL COMMENT 'A, AA, AAA',
  `CATEGORY`     varchar(30)  NOT NULL COMMENT 'VISUAL, INTERACTION, UX',
  `DESCRIPTION`  text         NOT NULL
);


-- ================================
-- 분석 결과
-- ================================

CREATE TABLE IF NOT EXISTS `ANALYSIS_WCAG_RESULTS` (
  `ANALYSIS_WCAG_RESULT_ID` bigint      PRIMARY KEY AUTO_INCREMENT,
  `REPOSITORY_ID`           bigint      NOT NULL,
  `WCAG_ITEM_ID`            bigint      NOT NULL,
  `STATUS`                  varchar(10) NOT NULL COMMENT 'PASS, FAIL, NA',
  `CREATED_AT`              datetime,
  `UPDATED_AT`              datetime,

  CONSTRAINT `FK_ANALYSIS_WCAG_RESULTS_REPOSITORY`
    FOREIGN KEY (`REPOSITORY_ID`) REFERENCES `REPOSITORIES` (`REPOSITORY_ID`),
  CONSTRAINT `FK_ANALYSIS_WCAG_RESULTS_WCAG_ITEM`
    FOREIGN KEY (`WCAG_ITEM_ID`) REFERENCES `WCAG_ITEMS` (`WCAG_ITEM_ID`)
);

CREATE TABLE IF NOT EXISTS `ANALYSIS_ISSUES` (
  `ANALYSIS_ISSUE_ID`       bigint      PRIMARY KEY AUTO_INCREMENT,
  `ANALYSIS_WCAG_RESULT_ID` bigint      NOT NULL,
  `STATUS`                  varchar(20) NOT NULL COMMENT 'OPEN, PROGRESS, COMPLETE, IGNORE',
  `CREATED_AT`              datetime,
  `UPDATED_AT`              datetime,

  CONSTRAINT `FK_ANALYSIS_ISSUES_WCAG_RESULT`
    FOREIGN KEY (`ANALYSIS_WCAG_RESULT_ID`)
    REFERENCES `ANALYSIS_WCAG_RESULTS` (`ANALYSIS_WCAG_RESULT_ID`)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `ANALYSIS_ISSUES_LOCATIONS` (
  `ANALYSIS_ISSUE_LOCATION_ID` bigint       PRIMARY KEY AUTO_INCREMENT,
  `ANALYSIS_ISSUE_ID`          bigint       NOT NULL,
  `TARGET_FILE_PATH`           varchar(255) NOT NULL,
  `TARGET_SELECTOR`            varchar(255),
  `ORIGINAL_CODE_BLOCK`        text         NOT NULL,
  `SUGGESTION`                 text         NOT NULL,
  `MEASURED_VALUE`             varchar(50),
  `THRESHOLD_VALUE`            varchar(50),
  `SUGGESTION_TYPE`            ENUM('COLOR_CONTRAST','CODE_FIX','ATTRIBUTE','LAYOUT','TEXT'),
  `SUGGESTION_DETAIL`          json,
  `STATUS`                     varchar(20)  NOT NULL COMMENT 'OPEN, MODIFY, COMPLETE',
  `CREATED_AT`                 datetime,
  `UPDATED_AT`                 datetime,

  CONSTRAINT `FK_ANALYSIS_ISSUE_LOCATIONS_ISSUE`
    FOREIGN KEY (`ANALYSIS_ISSUE_ID`) REFERENCES `ANALYSIS_ISSUES` (`ANALYSIS_ISSUE_ID`)
    ON DELETE CASCADE
);


-- ================================
-- 채팅
-- ================================

CREATE TABLE IF NOT EXISTS `CHAT_MESSAGES` (
  `CHAT_MESSAGE_ID` bigint       PRIMARY KEY AUTO_INCREMENT,
  `REPOSITORY_ID`   bigint       NOT NULL,
  `SENDER_TYPE`     varchar(10)  NOT NULL COMMENT 'USER, AI, SYSTEM',
  `CONTENT`         text         NOT NULL,
  `IMAGE_URL`       varchar(255),
  `CREATED_AT`      datetime,

  CONSTRAINT `FK_CHAT_MESSAGES_REPOSITORY`
    FOREIGN KEY (`REPOSITORY_ID`) REFERENCES `REPOSITORIES` (`REPOSITORY_ID`)
);
