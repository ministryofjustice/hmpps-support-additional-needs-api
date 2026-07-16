--- Add a new "General" strength type into the reference data table

INSERT INTO reference_data
    (id, domain, code, description, category_code, category_description, area_code, area_description, list_sequence, deactivated_at, default_for_category, screener_option)
    VALUES (
        gen_random_uuid(),
            'STRENGTH',
            'GENERAL_DEFAULT',
            'General',
            'GENERAL',
            'General',
            'GENERAL_NEED',
            'General Need',
            0,
            null,
            true,
            false
        );
