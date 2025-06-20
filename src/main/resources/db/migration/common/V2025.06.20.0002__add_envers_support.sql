ALTER TABLE plan_creation_schedule_history
DROP COLUMN version;

ALTER TABLE plan_creation_schedule_history
    ADD COLUMN version BIGSERIAL NOT NULL;

ALTER TABLE review_schedule_history
DROP COLUMN version;

ALTER TABLE review_schedule_history
    ADD COLUMN version BIGSERIAL NOT NULL;