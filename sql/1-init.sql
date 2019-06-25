DROP SCHEMA IF EXISTS timing CASCADE;

CREATE SCHEMA IF NOT EXISTS timing;

CREATE TABLE IF NOT EXISTS timing.Driver (
    id SERIAL UNIQUE NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS timing.Time (
    lap INTEGER NOT NULL,
    driver_id INTEGER NOT NULL,
    time VARCHAR(100) NOT NULL
);