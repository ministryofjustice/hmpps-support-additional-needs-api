-- create the two new tables:
-- ehcp status
-- ehcp status history
--
-- build the entries into history table
-- use the newly built history entries to create the main entity
--
-- Create new table to store EHCP status
CREATE TABLE ehcp_status
(
    id                       uuid                  PRIMARY KEY,
    reference                uuid                  NOT NULL,
    prison_number            varchar(10)           NOT NULL,
    has_current_ehcp         boolean DEFAULT false NOT NULL,
    created_at_prison        varchar(3)            NOT NULL,
    updated_at_prison        varchar(3)            NOT NULL,
    created_by               varchar(50)           NOT NULL,
    created_at               timestamp             NOT NULL,
    updated_by               varchar(50)           NOT NULL,
    updated_at               timestamp             NOT NULL
);

CREATE INDEX ehcp_status_prison_number_idx
    ON ehcp_status (prison_number);

-- Create new table to store EHCP status history
CREATE TABLE ehcp_status_history
(
    id                       uuid                  NOT NULL,
    reference                uuid                  NOT NULL,
    prison_number            varchar(10)           NOT NULL,
    has_current_ehcp         boolean DEFAULT false NOT NULL,
    created_at_prison        varchar(3)            NOT NULL,
    updated_at_prison        varchar(3)            NOT NULL,
    created_by               varchar(50)           NOT NULL,
    created_at               timestamp             NOT NULL,
    updated_by               varchar(50)           NOT NULL,
    updated_at               timestamp             NOT NULL,
    rev_type                 smallint,
    rev_id                   bigint                not null,
    primary key (rev_id, id)
);

comment on column ehcp_status_history.rev_type is
'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';

create index idx_ehcp_status_history_prison_number
    on ehcp_status_history (prison_number);

-- migrate existing history data
INSERT INTO ehcp_status_history (
    id,
    reference,
    prison_number,
    has_current_ehcp,
    created_at_prison,
    updated_at_prison,
    created_by,
    created_at,
    updated_by,
    updated_at,
    rev_type,
    rev_id
)
SELECT
    gen_random_uuid(),
    gen_random_uuid(),
    prison_number,
    has_current_ehcp,
    created_at_prison,
    updated_at_prison,
    created_by,
    created_at,
    updated_by,
    updated_at,
    rev_type,
    rev_id
FROM
    elsp_plan_history where rev_type = 0 ; --ie when the original entity was created

-- migrate existing entity data
INSERT INTO ehcp_status(
    id,
    reference,
    prison_number,
    has_current_ehcp,
    created_at_prison,
    updated_at_prison,
    created_by,
    created_at,
    updated_by,
    updated_at
)
SELECT
    id,
    reference,
    prison_number,
    has_current_ehcp,
    created_at_prison,
    updated_at_prison,
    created_by,
    created_at,
    updated_by,
    updated_at
FROM
    ehcp_status_history where rev_type = 0 ;

ALTER TABLE elsp_plan
DROP COLUMN has_current_ehcp;

ALTER TABLE elsp_plan_history
DROP COLUMN has_current_ehcp;