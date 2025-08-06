CREATE TABLE timeline (
          id                    UUID PRIMARY KEY,
          prison_number         VARCHAR NOT NULL,
          event                 VARCHAR NOT NULL,
          additional_info       TEXT,
          created_at            TIMESTAMP NOT NULL,
          created_by            VARCHAR(50) NOT NULL,
          created_at_prison     VARCHAR(3) NOT NULL,
);