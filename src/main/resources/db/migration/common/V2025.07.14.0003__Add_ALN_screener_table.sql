create table aln_screener
(
    id                   uuid                  not null
        primary key,
    reference            uuid                  not null,
    prison_number        varchar(10)           not null,
    created_at_prison    varchar(3)            not null,
    updated_at_prison    varchar(3),
    created_by           varchar(50),
    created_at           timestamp,
    updated_by           varchar(50),
    updated_at           timestamp,
    screening_date       date
);

create index aln_screener_prison_number_idx
    on aln_screener (prison_number);

alter table challenge drop column from_aln_screener;
alter table challenge drop column screening_date;

ALTER TABLE challenge
    ADD COLUMN aln_screener_id UUID;

ALTER TABLE challenge
    ADD CONSTRAINT fk_challenge_aln_screener
        FOREIGN KEY (aln_screener_id) REFERENCES aln_screener(id);


alter table strength drop column from_aln_screener;
alter table strength drop column screening_date;

ALTER TABLE strength
    ADD COLUMN aln_screener_id UUID;

ALTER TABLE strength
    ADD CONSTRAINT fk_strength_aln_screener
        FOREIGN KEY (aln_screener_id) REFERENCES aln_screener(id);



