create table review_schedule
(
    id                        uuid                        not null
        primary key,
    reference                 uuid                        not null,
    prison_number             varchar(10)                 not null,
    deadline_date             date                        not null,
    status                    varchar(50)                 not null,
    created_at                timestamp                   not null,
    created_by                varchar(50)                 not null,
    created_at_prison         varchar(3)                  not null,
    updated_at                timestamp                   not null,
    updated_by                varchar(50)                 not null,
    updated_at_prison         varchar(3)                  not null,
    exemption_reason          varchar(512)
);

create index review_schedule_prison_number_idx
    on public.review_schedule (prison_number);

create unique index review_schedule_reference_idx
    on public.review_schedule (reference);


create table plan_creation_schedule
(
    id                        uuid                        not null
        primary key,
    reference                 uuid                        not null,
    prison_number             varchar(10)                 not null,
    deadline_date             date                        not null,
    status                    varchar(50)                 not null,
    created_at                timestamp                   not null,
    created_by                varchar(50)                 not null,
    created_at_prison         varchar(3)                  not null,
    updated_at                timestamp                   not null,
    updated_by                varchar(50)                 not null,
    updated_at_prison         varchar(3)                  not null,
    exemption_reason          varchar(512)
);

create index plan_creation_schedule_prison_number_idx
    on plan_creation_schedule (prison_number);

create unique index plan_creation_schedule_reference_idx
    on plan_creation_schedule (reference);




