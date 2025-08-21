create table elsp_plan_history
(
    id                       uuid                  not null,
    reference                uuid                  not null,
    prison_number            varchar(10)           not null,
    has_current_ehcp         boolean default false not null,
    plan_created_by_name     varchar(200),
    plan_created_by_job_role varchar(200),
    teaching_adjustments     text,
    specific_teaching_skills text,
    exam_access_arrangements text,
    lnsp_support             text,
    created_at_prison        varchar(3)            not null,
    updated_at_prison        varchar(3)            not null,
    created_by               varchar(50)           not null,
    created_at               timestamp             not null,
    updated_by               varchar(50)           not null,
    updated_at               timestamp             not null,
    detail                   text,
    individual_support       text                  not null,
    lnsp_support_hours       integer,
    rev_type                 smallint,
    rev_id                   bigint                not null,
    primary key (rev_id, id)
);

comment on column elsp_plan_history.rev_type is
'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';

create index idx_elsp_plan_history_prison_number
    on elsp_plan_history (prison_number);


create table other_contributor_history
(
    id                uuid         not null,
    reference         uuid         not null,
    elsp_plan_id      uuid         not null,
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

comment on column other_contributor_history.rev_type is
'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';

create index idx_other_contributor_history_elsp_plan_id
    on other_contributor_history (elsp_plan_id);

create index idx_other_contributor_history_reference
    on other_contributor_history (reference);