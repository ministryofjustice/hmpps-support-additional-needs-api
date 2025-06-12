ALTER TABLE reference_data
    ADD COLUMN default_for_category boolean default false,
    ADD COLUMN screener_option boolean default false;

update reference_data set screener_option = true where domain = 'CHALLENGE';
update reference_data set default_for_category = false;
update reference_data set default_for_category = true where domain = 'CHALLENGE' and code = 'MEMORY';
update reference_data set default_for_category = true where domain = 'CHALLENGE' and code = 'PROCESSING_SPEED';


INSERT INTO reference_data (
    id, domain, code, description,
    category_code, category_description,
    area_code, area_description, screener_option, default_for_category,
    list_sequence
)
VALUES
    (gen_random_uuid(), 'CHALLENGE', 'LITERACY_SKILLS_DEFAULT', 'Literacy Skills', 'LITERACY_SKILLS', 'Literacy Skills', 'COGNITION_LEARNING', 'Cognition & Learning',false, true, 0),

    (gen_random_uuid(), 'CHALLENGE', 'NUMERACY_SKILLS_DEFAULT', 'Numeracy Skills', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', false, true, 0),

    (gen_random_uuid(), 'CHALLENGE', 'ATTENTION_ORGANISING_TIME_DEFAULT', 'Attention, Organising & Time Management', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', false, true, 0),

    (gen_random_uuid(), 'CHALLENGE', 'LANGUAGE_COMM_SKILLS_DEFAULT', 'Language & Communication skills', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', false, true, 0),

    (gen_random_uuid(), 'CHALLENGE', 'EMOTIONS_FEELINGS_DEFAULT', 'Emotions & feelings', 'EMOTIONS_FEELINGS', 'Emotions & feelings', 'SOCIAL_EMOTIONAL_MENTAL', 'Social, Emotional & Mental Health', false, true, 0),

    (gen_random_uuid(), 'CHALLENGE', 'PHYSICAL_SKILLS_DEFAULT', 'Physical Skills & coordination', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', false, true, 0),

    (gen_random_uuid(), 'CHALLENGE', 'SENSORY', 'Sensory', 'SENSORY', 'Sensory', 'PHYSICAL_SENSORY', 'Physical & Sensory', false, true, 0);

