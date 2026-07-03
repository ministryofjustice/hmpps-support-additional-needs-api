-- Create new table to record data deletion events.
--
-- By design, this table does not and must not record any reference to the data that was deleted, including the type of data.
-- This has been mandated by Legal/Data Protection, as the core requirement for deleting data is that the prisoner has not
-- consented to the data being stored. Therefore the data must be deleted and not retained in any form.
-- However, in order to satisfy a reporting requirement from the stakeholders, we need to record something about the deletion event.
-- The schema and data defined here has been agreed with Legal/Data Protection and satisifies both requirements.

CREATE TABLE data_deletion_event
(
    id                       uuid                  PRIMARY KEY,
    correlation_id           uuid                  NOT NULL,
    prison_number            varchar(10)           NOT NULL,
    reason                   varchar(50)           NOT NULL,
    deleted_at               timestamp             NOT NULL,
    deleted_by               varchar(50)           NOT NULL,
    deleted_at_prison        varchar(3)            NOT NULL
);
