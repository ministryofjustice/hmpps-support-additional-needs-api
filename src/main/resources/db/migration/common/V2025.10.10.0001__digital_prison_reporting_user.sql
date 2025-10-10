-- Create a DPR/Data Hub user based on https://dsdmoj.atlassian.net/wiki/spaces/DPR/pages/4461494352/Configure+DPS+RDS+Service+for+DPR+Ingestion#3.-Setup-Postgres-User

CREATE ROLE ${dpr_user} WITH LOGIN PASSWORD '${dpr_password}';

-- The rds_superuser role only exists in RDS postgres instances. This grant fails when running locally or as part of the integration tests, hence the if check
DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rds_superuser')
        THEN GRANT rds_superuser to ${dpr_user};
    END IF;
END$$;

-- The rds_replication role only exists in RDS postgres instances. This grant fails when running locally or as part of the integration tests, hence the if check
DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rds_replication')
        THEN GRANT rds_replication to ${dpr_user};
    END IF;
END$$;

