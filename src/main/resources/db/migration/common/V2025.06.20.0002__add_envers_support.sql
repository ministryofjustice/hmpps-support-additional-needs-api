ALTER TABLE review_schedule
    ADD COLUMN version int not null;

ALTER TABLE review_schedule_history
    ADD COLUMN rev_id bigint not null;

ALTER TABLE review_schedule_history
DROP
CONSTRAINT review_schedule_history_pkey;

ALTER TABLE review_schedule_history
    ADD PRIMARY KEY (rev_id, id);

ALTER TABLE plan_creation_schedule
    ADD COLUMN version int not null;

ALTER TABLE plan_creation_schedule_history
    ADD COLUMN rev_id bigint not null;

ALTER TABLE plan_creation_schedule_history
DROP
CONSTRAINT plan_creation_schedule_history_pkey;

ALTER TABLE plan_creation_schedule_history
    ADD PRIMARY KEY (rev_id, id);
