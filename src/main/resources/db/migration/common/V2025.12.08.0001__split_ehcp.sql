-- Create new table to store EHCP
CREATE TABLE ehcp_status
(
    id                       uuid                  PRIMARY KEY DEFAULT gen_random_uuid(),
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

-- migrate existing data
INSERT INTO ehcp_status (
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
    gen_random_uuid(),
    prison_number,
    has_current_ehcp,
    created_at_prison,
    updated_at_prison,
    created_by,
    created_at,
    updated_by,
    updated_at
FROM
    elsp_plan_history where rev_type = 0 ; --ie when the original entity was created


ALTER TABLE elsp_plan
DROP COLUMN has_current_ehcp;

ALTER TABLE elsp_plan_history
DROP COLUMN has_current_ehcp;