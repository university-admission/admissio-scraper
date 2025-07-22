CREATE TABLE universities (
    id SERIAL PRIMARY KEY,
    university_name VARCHAR(255) NOT NULL,
    university_code INTEGER NOT NULL UNIQUE,
    university_region_id INTEGER NOT NULL REFERENCES university_regions(id)
);