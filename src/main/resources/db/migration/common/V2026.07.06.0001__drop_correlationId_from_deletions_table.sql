-- Drop correlation_id from data_deletion_event table

ALTER TABLE data_deletion_event
    DROP COLUMN correlation_id;
