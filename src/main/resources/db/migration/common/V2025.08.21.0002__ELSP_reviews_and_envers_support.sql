create table elsp_review
(
    id                         UUID PRIMARY KEY,
    reference                  uuid                  not null,
    prison_number              varchar(10)           not null,
    prisoner_declined_feedback boolean default false not null,
    review_created_by_name     varchar(200),
    review_created_by_job_role varchar(200),
    prisoner_feedback          text,
    reviewer_feedback          text,
    created_at_prison          varchar(3)            not null,
    updated_at_prison          varchar(3)            not null,
    created_by                 varchar(50)           not null,
    created_at                 timestamp             not null,
    updated_by                 varchar(50)           not null,
    updated_at                 timestamp             not null,
    review_schedule_reference  uuid                  not null
);

CREATE TABLE other_review_contributor
(
    id                UUID PRIMARY KEY NOT NULL,
    reference         UUID             NOT NULL,
    elsp_review_id    UUID             NOT NULL REFERENCES elsp_review (id) ON DELETE CASCADE,
    name              VARCHAR(200)     NOT NULL,
    job_role          VARCHAR(200)     NOT NULL,
    created_at_prison VARCHAR(3)       NOT NULL,
    updated_at_prison VARCHAR(3)       NOT NULL,
    created_by        VARCHAR(50)      NOT NULL,
    created_at        TIMESTAMP        NOT NULL,
    updated_by        VARCHAR(50)      NOT NULL,
    updated_at        TIMESTAMP        NOT NULL
);

create index idx_elsp_review_prison_number
    on elsp_review (prison_number);

alter table elsp_review
    add constraint fk_elsp_review_review_schedule_reference
        foreign key (review_schedule_reference)
            references review_schedule (reference);

--envers version:

create table elsp_review_history
(
    id                         uuid                  not null,
    reference                  uuid                  not null,
    prison_number              varchar(10)           not null,
    prisoner_declined_feedback boolean default false not null,
    review_created_by_name     varchar(200),
    review_created_by_job_role varchar(200),
    prisoner_feedback          text,
    reviewer_feedback          text,
    created_at_prison          varchar(3)            not null,
    updated_at_prison          varchar(3)            not null,
    created_by                 varchar(50)           not null,
    created_at                 timestamp             not null,
    updated_by                 varchar(50)           not null,
    updated_at                 timestamp             not null,
    review_schedule_reference  uuid                  not null,
    rev_type                   smallint,
    rev_id                     bigint                not null,
    primary key (rev_id, id)
);

create table other_review_contributor_history
(
    id                uuid         not null,
    reference         uuid         not null,
    elsp_review_id    uuid         not null,
    name              varchar(200) not null,
    job_role          varchar(200) not null,
    created_at_prison varchar(3)   not null,
    updated_at_prison varchar(3)   not null,
    created_by        varchar(50)  not null,
    created_at        timestamp    not null,
    updated_by        varchar(50)  not null,
    updated_at        timestamp    not null,
    rev_type          smallint,
    rev_id            bigint       not null,
    primary key (rev_id, id)
);

comment
on column other_review_contributor_history.rev_type is
'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';

create index idx_other_review_contributor_history_elsp_review_id
    on other_review_contributor_history (elsp_review_id);

create index idx_other_review_contributor_history_reference
    on other_review_contributor_history (reference);