CREATE TABLE education
(
    id                               UUID PRIMARY KEY NOT NULL,
    reference                        UUID             NOT NULL,
    prison_number                    VARCHAR(10)      NOT NULL,
    in_education                     BOOLEAN DEFAULT FALSE NOT NULL,
    curious_reference                UUID,
    created_by        VARCHAR(50)    NOT NULL,
    created_at        TIMESTAMP      NOT NULL,
    updated_by        VARCHAR(50)    NOT NULL,
    updated_at        TIMESTAMP      NOT NULL
);


create index education_prison_number_idx
    on education (prison_number);


CREATE TABLE aln_assessment
(
    id                               UUID PRIMARY KEY NOT NULL,
    reference                        UUID             NOT NULL,
    prison_number                    VARCHAR(10)      NOT NULL,
    has_need                         BOOLEAN DEFAULT FALSE NOT NULL,
    curious_reference                UUID,
    created_by        VARCHAR(50)    NOT NULL,
    created_at        TIMESTAMP      NOT NULL,
    updated_by        VARCHAR(50)    NOT NULL,
    updated_at        TIMESTAMP      NOT NULL
);


create index aln_assessment_number_idx
    on aln_assessment (prison_number);


CREATE TABLE ldd_assessment
(
    id                               UUID PRIMARY KEY NOT NULL,
    reference                        UUID             NOT NULL,
    prison_number                    VARCHAR(10)      NOT NULL,
    has_need                         BOOLEAN DEFAULT FALSE NOT NULL,
    created_by        VARCHAR(50)    NOT NULL,
    created_at        TIMESTAMP      NOT NULL,
    updated_by        VARCHAR(50)    NOT NULL,
    updated_at        TIMESTAMP      NOT NULL
);


create index ldd_assessment_number_idx
    on ldd_assessment (prison_number);