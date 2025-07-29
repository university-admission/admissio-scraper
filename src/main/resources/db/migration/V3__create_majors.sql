CREATE TABLE majors (
    id BIGSERIAL PRIMARY KEY,
    major_name VARCHAR(255) NOT NULL,
    major_code VARCHAR(255) NOT NULL UNIQUE,
    uk_lang_coef DOUBLE PRECISION NOT NULL,
    math_coef DOUBLE PRECISION NOT NULL,
    history_coef DOUBLE PRECISION NOT NULL,
    uk_literature_coef DOUBLE PRECISION NOT NULL,
    foreign_lang_coef DOUBLE PRECISION NOT NULL,
    biology_coef DOUBLE PRECISION NOT NULL,
    geography_coef DOUBLE PRECISION NOT NULL,
    physics_coef DOUBLE PRECISION NOT NULL,
    chemistry_coef DOUBLE PRECISION NOT NULL,
    competition_coef DOUBLE PRECISION NOT NULL,
    major_coef DOUBLE PRECISION NOT NULL CHECK (major_coef >= 1 AND major_coef <= 2)
);