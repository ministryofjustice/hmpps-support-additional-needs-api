ALTER TABLE challenge
DROP CONSTRAINT IF EXISTS fk_challenge_aln_screener;

ALTER TABLE challenge
    ADD CONSTRAINT fk_challenge_aln_screener
        FOREIGN KEY (aln_screener_id)
            REFERENCES aln_screener(id)
            ON DELETE CASCADE;

ALTER TABLE strength
DROP CONSTRAINT IF EXISTS fk_strength_aln_screener;

ALTER TABLE strength
    ADD CONSTRAINT fk_strength_aln_screener
        FOREIGN KEY (aln_screener_id)
            REFERENCES aln_screener(id)
            ON DELETE CASCADE;