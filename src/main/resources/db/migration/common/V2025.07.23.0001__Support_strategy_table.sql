create table support_strategy
(
    id                uuid                 not null
        primary key,
    reference         uuid                 not null,
    prison_number     varchar(10)          not null,
    support_strategy_type_id uuid          not null
        constraint fk_support_strategy_type
            references reference_data,
    active            boolean default true not null,
    created_at_prison varchar(3)           not null,
    updated_at_prison varchar(3),
    created_by        varchar(50),
    created_at        timestamp,
    updated_by        varchar(50),
    updated_at        timestamp,
    detail            text
);


create index support_strategy_prison_number_idx
    on support_strategy (prison_number);

