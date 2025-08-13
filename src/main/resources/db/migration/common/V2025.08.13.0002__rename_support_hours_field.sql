ALTER TABLE elsp_plan
    RENAME COLUMN individual_support_hours to lnsp_support_hours;

ALTER TABLE elsp_plan
    ALTER COLUMN lnsp_support_hours DROP NOT NULL;
ALTER TABLE elsp_plan
    ALTER COLUMN lnsp_support_hours DROP DEFAULT;
