create table audit_revision
(
    id                  bigserial    not null primary key,
    timestamp           timestamp    not null
);

ALTER TABLE review_schedule_history
    ADD COLUMN rev_type smallint;

ALTER TABLE review_schedule_history
DROP
CONSTRAINT review_schedule_history_pkey;

ALTER TABLE review_schedule_history
    ADD PRIMARY KEY (version, id);

ALTER TABLE plan_creation_schedule_history
    ADD COLUMN rev_type smallint ;

ALTER TABLE plan_creation_schedule_history
DROP
CONSTRAINT plan_creation_schedule_history_pkey;

ALTER TABLE plan_creation_schedule_history
    ADD PRIMARY KEY (version, id);


COMMENT ON COLUMN plan_creation_schedule_history.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN review_schedule_history.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';