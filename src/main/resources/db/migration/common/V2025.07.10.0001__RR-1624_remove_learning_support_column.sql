-- V2025.07.04.0003__Add_individual_support_field_field_constraint.sql
--
-- Set the non-null constraint on individual_support
--
-- See the following 3 scripts which should run in this order:
--   /db/migration/common/V2025.07.04.0001__Add_individual_support_field_to_ELSP_PLAN.sql
--   /db/migration/dev/V2025.07.04.0002__Set_individual_support_field_for_dev_records.sql
--   /db/migration/common/V2025.07.04.0003__Add_individual_support_field_constraint.sql

ALTER TABLE ELSP_PLAN
    DROP COLUMN learning_environment_adjustments;
