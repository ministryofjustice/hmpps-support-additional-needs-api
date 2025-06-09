CREATE TABLE challenge
(
    id                UUID PRIMARY KEY NOT NULL,
    reference         UUID             NOT NULL,
    prison_number     varchar(10)      NOT NULL,
    from_aln_screener BOOLEAN          NOT NULL DEFAULT FALSE,
    challenge_type_id UUID             NOT NULL,
    active            BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at_prison varchar(3)       NOT NULL,
    updated_at_prison varchar(3),

    created_by        varchar(50),
    created_at        TIMESTAMP,
    updated_by        varchar(50),
    updated_at        TIMESTAMP,

    CONSTRAINT fk_challenge_type
        FOREIGN KEY (challenge_type_id)
            REFERENCES reference_data (id)
);

create index challenge_prison_number_idx
    on challenge (prison_number);