
CREATE TABLE ELSP_PLAN
(
    id                               UUID PRIMARY KEY NOT NULL,
    reference                        UUID             NOT NULL,
    prison_number                    VARCHAR(10)      NOT NULL,
    has_current_ehcp                 BOOLEAN DEFAULT FALSE NOT NULL,
    plan_created_by_name             VARCHAR(200),
    plan_created_by_job_role         VARCHAR(200),
    learning_environment_adjustments TEXT,
    teaching_adjustments             TEXT,
    specific_teaching_skills         TEXT,
    exam_access_arrangements         TEXT,
    lnsp_support                     TEXT,
    created_at_prison VARCHAR(3)     NOT NULL,
    updated_at_prison VARCHAR(3)     NOT NULL,
    created_by        VARCHAR(50)    NOT NULL,
    created_at        TIMESTAMP      NOT NULL,
    updated_by        VARCHAR(50)    NOT NULL,
    updated_at        TIMESTAMP      NOT NULL
);

CREATE TABLE OTHER_CONTRIBUTOR
(
    id                               UUID PRIMARY KEY NOT NULL,
    reference                        UUID             NOT NULL,
    elsp_plan_id UUID                NOT NULL REFERENCES elsp_plan (id) ON DELETE CASCADE,
    name                             VARCHAR(200) NOT NULL,
    job_role                         VARCHAR(200) NOT NULL,
    created_at_prison VARCHAR(3)     NOT NULL,
    updated_at_prison VARCHAR(3)     NOT NULL,
    created_by        VARCHAR(50)    NOT NULL,
    created_at        TIMESTAMP      NOT NULL,
    updated_by        VARCHAR(50)    NOT NULL,
    updated_at        TIMESTAMP      NOT NULL
);

create index elsp_plan_prison_number_idx
    on ELSP_PLAN (prison_number);
