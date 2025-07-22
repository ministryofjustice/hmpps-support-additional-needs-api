
ALTER TABLE aln_screener
    ADD COLUMN "has_challenges" BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN "has_strengths" BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE aln_screener a
SET has_challenges = EXISTS (
    SELECT 1 FROM challenge c
    WHERE c.aln_screener_id = a.id
);

UPDATE aln_screener a
SET has_strengths = EXISTS (
    SELECT 1 FROM strength s
    WHERE s.aln_screener_id = a.id
);

ALTER TABLE aln_screener
    ALTER COLUMN "has_challenges" DROP DEFAULT,
    ALTER COLUMN "has_strengths" DROP DEFAULT;