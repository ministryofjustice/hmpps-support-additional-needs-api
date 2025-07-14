create table strength
(
    id                   uuid                  not null
        primary key,
    reference            uuid                  not null,
    prison_number        varchar(10)           not null,
    from_aln_screener    boolean default false not null,
    strength_type_id    uuid                  not null
        constraint fk_strength_type
            references reference_data,
    active               boolean default true  not null,
    created_at_prison    varchar(3)            not null,
    updated_at_prison    varchar(3),
    created_by           varchar(50),
    created_at           timestamp,
    updated_by           varchar(50),
    updated_at           timestamp,
    symptoms             text,
    how_identified_other text,
    screening_date       date,
    how_identified       character varying[]
);

create index strength_prison_number_idx
    on strength (prison_number);

