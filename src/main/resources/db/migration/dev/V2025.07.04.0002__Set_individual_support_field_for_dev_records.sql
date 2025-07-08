-- V2025.07.04.0002__Set_individual_support_field_for_dev_records.sql
--
-- Set a value for individual_support on all records in dev. Need to do this before the non-null constraint is added to
-- the column.
--
-- See the following 3 scripts which should run in this order:
--   /db/migration/common/V2025.07.04.0001__Add_individual_support_field_to_ELSP_PLAN.sql
--   /db/migration/dev/V2025.07.04.0002__Set_individual_support_field_for_dev_records.sql
--   /db/migration/common/V2025.07.04.0003__Add_individual_support_field_constraint.sql

UPDATE ELSP_PLAN
    SET individual_support = 'No special needs requested';
