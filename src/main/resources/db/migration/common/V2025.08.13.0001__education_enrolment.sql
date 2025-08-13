CREATE TABLE education_enrolment
(
    id                     UUID PRIMARY KEY,
    reference              UUID,
    prison_number          TEXT        NOT NULL,
    establishment_id       TEXT        NOT NULL,
    qualification_code     TEXT        NOT NULL,
    learning_start_date    DATE        NOT NULL,
    funding_type           TEXT,
    completion_status      TEXT,
    planned_end_date       DATE,
    end_date               DATE,
    last_curious_reference UUID,
    created_by             VARCHAR(50) NOT NULL,
    created_at             TIMESTAMP   NOT NULL,
    updated_by             VARCHAR(50) NOT NULL,
    updated_at             TIMESTAMP   NOT NULL
);

-- Since there is no unique id from Curious I've had to make one
ALTER TABLE education_enrolment
    ADD CONSTRAINT uq_enrolment_unique_per_course
        UNIQUE (prison_number, establishment_id, qualification_code, learning_start_date);

CREATE INDEX education_enrolment_prison_number_idx
    ON education_enrolment (prison_number);

CREATE INDEX education_enrolment_prison_active_idx
    ON education_enrolment (prison_number, end_date);