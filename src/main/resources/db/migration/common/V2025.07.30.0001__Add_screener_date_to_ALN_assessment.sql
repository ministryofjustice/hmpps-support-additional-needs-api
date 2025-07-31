ALTER TABLE aln_assessment
    ADD COLUMN screening_date DATE;

UPDATE aln_assessment
SET screening_date = '2025-01-01';  --the only ones in the database are from test data harness

ALTER TABLE aln_assessment
    ALTER COLUMN screening_date SET NOT NULL;


