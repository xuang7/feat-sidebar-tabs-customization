-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

-- ============================================
-- 1. Drop and recreate the database (psql only)
--    Remove if you already created texera_db
-- ============================================
\c postgres
DROP DATABASE IF EXISTS texera_db;
CREATE DATABASE texera_db;

-- ============================================
-- 2. Connect to the new database (psql only)
-- ============================================
\c texera_db

CREATE SCHEMA IF NOT EXISTS texera_db;
SET search_path TO texera_db, public;

-- ============================================
-- 3. Drop all tables if they exist
--    (CASCADE handles FK dependencies)
-- ============================================
DROP TABLE IF EXISTS operator_executions CASCADE;
DROP TABLE IF EXISTS operator_port_executions CASCADE;
DROP TABLE IF EXISTS workflow_user_access CASCADE;
DROP TABLE IF EXISTS workflow_of_user CASCADE;
DROP TABLE IF EXISTS user_config CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;
DROP TABLE IF EXISTS workflow CASCADE;
DROP TABLE IF EXISTS workflow_version CASCADE;
DROP TABLE IF EXISTS project CASCADE;
DROP TABLE IF EXISTS workflow_of_project CASCADE;
DROP TABLE IF EXISTS workflow_executions CASCADE;
DROP TABLE IF EXISTS dataset CASCADE;
DROP TABLE IF EXISTS dataset_user_access CASCADE;
DROP TABLE IF EXISTS dataset_version CASCADE;
DROP TABLE IF EXISTS public_project CASCADE;
DROP TABLE IF EXISTS project_user_access CASCADE;
DROP TABLE IF EXISTS workflow_user_likes CASCADE;
DROP TABLE IF EXISTS workflow_user_clones CASCADE;
DROP TABLE IF EXISTS workflow_view_count CASCADE;
DROP TABLE IF EXISTS workflow_user_activity CASCADE;
DROP TABLE IF EXISTS user_activity CASCADE;
DROP TABLE IF EXISTS dataset_user_likes CASCADE;
DROP TABLE IF EXISTS dataset_view_count CASCADE;
DROP TABLE IF EXISTS site_settings CASCADE;
DROP TABLE IF EXISTS computing_unit_user_access CASCADE;

-- ============================================
-- 4. Create PostgreSQL enum types
--    to mimic MySQL ENUM fields
-- ============================================
DROP TYPE IF EXISTS user_role_enum CASCADE;
DROP TYPE IF EXISTS privilege_enum CASCADE;

CREATE TYPE user_role_enum AS ENUM ('INACTIVE', 'RESTRICTED', 'REGULAR', 'ADMIN');
CREATE TYPE privilege_enum AS ENUM ('NONE', 'READ', 'WRITE');
CREATE TYPE workflow_computing_unit_type_enum AS ENUM ('local', 'kubernetes');

-- ============================================
-- 5. Create tables
-- ============================================

-- "user" table
CREATE TABLE IF NOT EXISTS "user"
(
    uid           SERIAL PRIMARY KEY,
    name          VARCHAR(256) NOT NULL,
    email         VARCHAR(256) UNIQUE,
    password      VARCHAR(256),
    google_id     VARCHAR(256) UNIQUE,
    google_avatar VARCHAR(100),
    role          user_role_enum NOT NULL DEFAULT 'INACTIVE',
    comment TEXT,
    -- check that either password or google_id is not null
    CONSTRAINT ck_nulltest CHECK ((password IS NOT NULL) OR (google_id IS NOT NULL))
    );

-- user_config
CREATE TABLE IF NOT EXISTS user_config
(
    uid   INT NOT NULL,
    key   VARCHAR(256) NOT NULL,
    value TEXT NOT NULL,
    PRIMARY KEY (uid, key),
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE
    );

-- workflow
CREATE TABLE IF NOT EXISTS workflow
(
    wid                SERIAL PRIMARY KEY,
    name               VARCHAR(128) NOT NULL,
    description        VARCHAR(500),
    content            TEXT NOT NULL,
    creation_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_public          BOOLEAN NOT NULL DEFAULT false
    );

-- workflow_of_user
CREATE TABLE IF NOT EXISTS workflow_of_user
(
    uid INT NOT NULL,
    wid INT NOT NULL,
    PRIMARY KEY (uid, wid),
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE,
    FOREIGN KEY (wid) REFERENCES workflow(wid) ON DELETE CASCADE
    );

-- workflow_user_access
CREATE TABLE IF NOT EXISTS workflow_user_access
(
    uid       INT NOT NULL,
    wid       INT NOT NULL,
    privilege privilege_enum NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (uid, wid),
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE,
    FOREIGN KEY (wid) REFERENCES workflow(wid) ON DELETE CASCADE
    );

-- workflow_version
CREATE TABLE IF NOT EXISTS workflow_version
(
    vid            SERIAL PRIMARY KEY,
    wid            INT NOT NULL,
    content        TEXT NOT NULL,
    creation_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wid) REFERENCES workflow(wid) ON DELETE CASCADE
    );

-- project
CREATE TABLE IF NOT EXISTS project
(
    pid            SERIAL PRIMARY KEY,
    name           VARCHAR(128) NOT NULL,
    description    VARCHAR(10000),
    owner_id       INT NOT NULL,
    creation_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    color          VARCHAR(6),
    UNIQUE (owner_id, name),
    FOREIGN KEY (owner_id) REFERENCES "user"(uid) ON DELETE CASCADE
    );

-- workflow_of_project
CREATE TABLE IF NOT EXISTS workflow_of_project
(
    wid INT NOT NULL,
    pid INT NOT NULL,
    PRIMARY KEY (wid, pid),
    FOREIGN KEY (wid) REFERENCES workflow(wid) ON DELETE CASCADE,
    FOREIGN KEY (pid) REFERENCES project(pid) ON DELETE CASCADE
    );

-- project_user_access
CREATE TABLE IF NOT EXISTS project_user_access
(
    uid       INT NOT NULL,
    pid       INT NOT NULL,
    privilege privilege_enum NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (uid, pid),
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE,
    FOREIGN KEY (pid) REFERENCES project(pid) ON DELETE CASCADE
    );

-- workflow_computing_unit table
CREATE TABLE IF NOT EXISTS workflow_computing_unit
(
    uid                INT           NOT NULL,
    name               VARCHAR(128)  NOT NULL,
    cuid               SERIAL PRIMARY KEY,
    creation_time      TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    terminate_time     TIMESTAMP  DEFAULT NULL,
    type               workflow_computing_unit_type_enum,
    uri                TEXT NOT NULL DEFAULT '',
    resource           TEXT DEFAULT '',
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE
);

-- workflow_executions
CREATE TABLE IF NOT EXISTS workflow_executions
(
    eid                 SERIAL PRIMARY KEY,
    vid                 INT NOT NULL,
    uid                 INT NOT NULL,
    cuid                INT,
    status              SMALLINT NOT NULL DEFAULT 1,
    result              TEXT,
    starting_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_update_time    TIMESTAMP,
    bookmarked          BOOLEAN DEFAULT FALSE,
    name                VARCHAR(128) NOT NULL DEFAULT 'Untitled Execution',
    environment_version VARCHAR(128) NOT NULL,
    log_location        TEXT,
    runtime_stats_uri   TEXT,
    runtime_stats_size  INT DEFAULT 0,
    FOREIGN KEY (vid) REFERENCES workflow_version(vid) ON DELETE CASCADE,
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE,
    FOREIGN KEY (cuid) REFERENCES workflow_computing_unit(cuid) ON DELETE CASCADE
);

-- public_project
CREATE TABLE IF NOT EXISTS public_project
(
    pid INT PRIMARY KEY,
    uid INT,
    FOREIGN KEY (pid) REFERENCES project(pid) ON DELETE CASCADE
    -- Note: MySQL schema doesn't define a foreign key for uid
    );

-- dataset
CREATE TABLE IF NOT EXISTS dataset
(
    did            SERIAL PRIMARY KEY,
    owner_uid      INT NOT NULL,
    name           VARCHAR(128) NOT NULL,
    is_public      BOOLEAN NOT NULL DEFAULT TRUE,
    description    VARCHAR(512) NOT NULL,
    creation_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_uid) REFERENCES "user"(uid) ON DELETE CASCADE
    );

-- dataset_user_access
CREATE TABLE IF NOT EXISTS dataset_user_access
(
    did       INT NOT NULL,
    uid       INT NOT NULL,
    privilege privilege_enum NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (did, uid),
    FOREIGN KEY (did) REFERENCES dataset(did) ON DELETE CASCADE,
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE
    );

-- dataset_version
CREATE TABLE IF NOT EXISTS dataset_version
(
    dvid          SERIAL PRIMARY KEY,
    did           INT NOT NULL,
    creator_uid   INT NOT NULL,
    name          VARCHAR(128) NOT NULL,
    version_hash  VARCHAR(64) NOT NULL,
    creation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (did) REFERENCES dataset(did) ON DELETE CASCADE
    );

-- operator_executions (modified to match MySQL: no separate primary key; added console_messages_uri)
CREATE TABLE IF NOT EXISTS operator_executions
(
    workflow_execution_id INT NOT NULL,
    operator_id           VARCHAR(100) NOT NULL,
    console_messages_uri  TEXT,
    console_messages_size INT DEFAULT 0,
    PRIMARY KEY (workflow_execution_id, operator_id),
    FOREIGN KEY (workflow_execution_id) REFERENCES workflow_executions(eid) ON DELETE CASCADE
    );

-- operator_port_executions
CREATE TABLE operator_port_executions
(
    workflow_execution_id INT NOT NULL,
    global_port_id        VARCHAR(200) NOT NULL,
    result_uri            TEXT,
    result_size           INT DEFAULT 0,
    PRIMARY KEY (workflow_execution_id, global_port_id),
    FOREIGN KEY (workflow_execution_id) REFERENCES workflow_executions(eid) ON DELETE CASCADE
);

-- workflow_user_likes
CREATE TABLE IF NOT EXISTS workflow_user_likes
(
    uid INT NOT NULL,
    wid INT NOT NULL,
    PRIMARY KEY (uid, wid),
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE,
    FOREIGN KEY (wid) REFERENCES workflow(wid) ON DELETE CASCADE
    );

-- workflow_user_clones
CREATE TABLE IF NOT EXISTS workflow_user_clones
(
    uid INT NOT NULL,
    wid INT NOT NULL,
    PRIMARY KEY (uid, wid),
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE,
    FOREIGN KEY (wid) REFERENCES workflow(wid) ON DELETE CASCADE
    );

-- workflow_view_count
CREATE TABLE IF NOT EXISTS workflow_view_count
(
    wid        INT NOT NULL PRIMARY KEY,
    view_count INT NOT NULL DEFAULT 0,
    FOREIGN KEY (wid) REFERENCES workflow(wid) ON DELETE CASCADE
    );

-- Drop old workflow_user_activity (if any), replace with user_activity
-- user_activity table
CREATE TABLE IF NOT EXISTS user_activity
(
    uid           INTEGER NOT NULL DEFAULT 0,
    id            INTEGER NOT NULL,
    type          VARCHAR(15) NOT NULL,
    ip            VARCHAR(15) DEFAULT NULL,
    activate      VARCHAR(10) NOT NULL,
    activity_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- dataset_user_likes table
CREATE TABLE IF NOT EXISTS dataset_user_likes
(
    uid INTEGER NOT NULL,
    did INTEGER NOT NULL,
    PRIMARY KEY (uid, did),
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE,
    FOREIGN KEY (did) REFERENCES dataset(did) ON DELETE CASCADE
    );

-- dataset_view_count table
CREATE TABLE IF NOT EXISTS dataset_view_count
(
    did        INTEGER NOT NULL,
    view_count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (did),
    FOREIGN KEY (did) REFERENCES dataset(did) ON DELETE CASCADE
    );

-- site_settings table
CREATE TABLE IF NOT EXISTS site_settings
(
    key         VARCHAR(255) PRIMARY KEY,
    value       TEXT NOT NULL,
    updated_by  VARCHAR(50),
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- computing_unit_user_access table
CREATE TABLE IF NOT EXISTS computing_unit_user_access
(
    cuid      INT NOT NULL,
    uid       INT NOT NULL,
    privilege privilege_enum NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (cuid, uid),
    FOREIGN KEY (cuid) REFERENCES workflow_computing_unit(cuid) ON DELETE CASCADE,
    FOREIGN KEY (uid) REFERENCES "user"(uid) ON DELETE CASCADE
);

-- START Fulltext search index creation (DO NOT EDIT THIS LINE)
CREATE EXTENSION IF NOT EXISTS pgroonga;

DO $$
DECLARE
  r RECORD;
  stem_filter TEXT := '';
  plugin_status TEXT;
BEGIN
  -- Drop all GIN and PGroonga indexes
  FOR r IN
    SELECT indexname FROM pg_indexes
    WHERE (indexdef ILIKE '%USING gin%' OR indexdef ILIKE '%USING pgroonga%')
    AND tablename IN ('workflow', 'user', 'project', 'dataset', 'dataset_version')
  LOOP
    EXECUTE format('DROP INDEX IF EXISTS %I;', r.indexname);
  END LOOP;

  -- Check if TokenFilterStem plugin is registered
  WITH plugin_registration AS (
    SELECT pgroonga_command('plugin_register token_filters/stem') AS result
  )
  SELECT
    CASE
      WHEN result::jsonb @> '[true]' THEN 'Plugin registered successfully'
      ELSE 'Plugin registration failed'
    END INTO plugin_status
  FROM plugin_registration;

  -- Set the stem_filter based on plugin status
  IF plugin_status = 'Plugin registered successfully' THEN
    stem_filter := ', plugins=''token_filters/stem'', token_filters=''TokenFilterStem''';
    RAISE NOTICE 'Using TokenMecab + TokenFilterStem';
  ELSE
    RAISE NOTICE 'Using TokenMecab only';
  END IF;

  -- Create PGroonga indexes dynamically with correct TokenFilterStem usage
  FOR r IN
    SELECT tablename,
           CASE
             WHEN tablename = 'workflow' THEN
               '(COALESCE(name, '''') || '' '' || COALESCE(description, '''') || '' '' || COALESCE(content, ''''))'
             WHEN tablename IN ('project', 'dataset') THEN
               '(COALESCE(name, '''') || '' '' || COALESCE(description, ''''))'
             ELSE
               'COALESCE(name, '''')'
           END AS index_column
    FROM (VALUES ('workflow'), ('user'), ('project'), ('dataset'), ('dataset_version')) AS t(tablename)
  LOOP
    -- Create PGroonga index with proper TokenFilterStem usage
    EXECUTE format(
      'CREATE INDEX idx_%s_pgroonga ON %I USING pgroonga (%s) WITH (tokenizer = ''TokenMecab''%s);',
      r.tablename, r.tablename, r.index_column, stem_filter
    );
  END LOOP;
END $$;

-- END Fulltext search index creation (DO NOT EDIT THIS LINE)
