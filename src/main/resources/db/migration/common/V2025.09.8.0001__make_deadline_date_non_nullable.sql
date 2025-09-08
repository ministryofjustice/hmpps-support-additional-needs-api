UPDATE plan_creation_schedule
SET deadline_date = '2099-12-31'
WHERE deadline_date IS NULL;

ALTER TABLE plan_creation_schedule
    ALTER COLUMN deadline_date SET NOT NULL;

UPDATE plan_creation_schedule_history
SET deadline_date = '2099-12-31'
WHERE deadline_date IS NULL;

ALTER TABLE plan_creation_schedule_history
    ALTER COLUMN deadline_date SET NOT NULL;

UPDATE review_schedule
SET deadline_date = '2099-12-31'
WHERE deadline_date IS NULL;

ALTER TABLE review_schedule
    ALTER COLUMN deadline_date SET NOT NULL;

UPDATE review_schedule_history
SET deadline_date = '2099-12-31'
WHERE deadline_date IS NULL;

ALTER TABLE review_schedule_history
    ALTER COLUMN deadline_date SET NOT NULL;