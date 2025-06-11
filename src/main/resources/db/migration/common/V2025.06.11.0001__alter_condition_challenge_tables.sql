ALTER TABLE condition
    ADD COLUMN detail TEXT;

ALTER TABLE challenge
    ADD COLUMN symptoms TEXT,
    ADD COLUMN how_identified TEXT,
    ADD COLUMN screening_date DATE;