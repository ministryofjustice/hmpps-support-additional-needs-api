INSERT INTO reference_data (
    id, domain, code, description,
    category_code, category_description,
    area_code, area_description,
    list_sequence, default_for_category, screener_option
)
VALUES
    (gen_random_uuid(), 'CHALLENGE', 'SHORT_TERM_MEMORY', 'Short term memory', 'MEMORY', 'Memory', 'MEMORY', 'Memory', 1, false, true),
    (gen_random_uuid(), 'CHALLENGE', 'LONG_TERM_MEMORY', 'Long term memory', 'MEMORY', 'Memory', 'MEMORY', 'Memory', 2, false, true),
    (gen_random_uuid(), 'STRENGTH', 'SHORT_TERM_MEMORY', 'Short term memory', 'MEMORY', 'Memory', 'MEMORY', 'Memory', 1, false, true),
    (gen_random_uuid(), 'STRENGTH', 'LONG_TERM_MEMORY', 'Long term memory', 'MEMORY', 'Memory', 'MEMORY', 'Memory', 2, false, true);

update reference_data set category_list_sequence = 35 where category_code = 'MEMORY' and domain = 'CHALLENGE';
update reference_data set category_list_sequence = 35 where category_code = 'MEMORY' and domain = 'STRENGTH';
