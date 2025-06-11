DELETE FROM reference_data WHERE domain = 'CONDITION';

-- Autism Spectrum Disorder
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'ASC', 'Autism Spectrum Condition', 'AUTISM', 'Autism Spectrum Disorder', 1),
    (gen_random_uuid(), 'CONDITION', 'ASC_OTHER', 'Other', 'AUTISM', 'Autism Spectrum Disorder', 2);

-- Learning Difficulty
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'ADHD', 'Attention Deficit Hyperactivity Disorder (ADHD / ADD)', 'LEARNING_DIFFICULTY', 'Learning Difficulty', 1),
    (gen_random_uuid(), 'CONDITION', 'DYSLEXIA', 'Dyslexia', 'LEARNING_DIFFICULTY', 'Learning Difficulty', 2),
    (gen_random_uuid(), 'CONDITION', 'DYSPRAXIA', 'Dyspraxia / Developmental Coordination Disorder', 'LEARNING_DIFFICULTY', 'Learning Difficulty', 3),
    (gen_random_uuid(), 'CONDITION', 'DYSCALCULIA', 'Dyscalculia', 'LEARNING_DIFFICULTY', 'Learning Difficulty', 4),
    (gen_random_uuid(), 'CONDITION', 'LEARN_DIFF_OTHER', 'Other', 'LEARNING_DIFFICULTY', 'Learning Difficulty', 5);

-- Learning Disability
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'LD_DOWN', 'Down''s Syndrome', 'LEARNING_DISABILITY', 'Learning Disability', 1),
    (gen_random_uuid(), 'CONDITION', 'LD_OTHER', 'Other learning disability', 'LEARNING_DISABILITY', 'Learning Disability', 2);

-- Hearing, speech, language and / or communication impairment
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'DLD_HEAR', 'Hearing Impairment', 'COMMUNICATION_IMPAIRMENT', 'Hearing, speech, language and / or communication impairment', 1),
    (gen_random_uuid(), 'CONDITION', 'DLD_LANG', 'Developmental Language Disorder', 'COMMUNICATION_IMPAIRMENT', 'Hearing, speech, language and / or communication impairment', 2),
    (gen_random_uuid(), 'CONDITION', 'DLD_OTHER', 'Other Language / speech / communication disorder', 'COMMUNICATION_IMPAIRMENT', 'Hearing, speech, language and / or communication impairment', 3);

-- Visual Impairment
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'VISUAL_IMPAIR', 'Visual Impairment', 'VISUAL_IMPAIRMENT', 'Visual Impairment', 1);

-- Mental Health
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'MENTAL_HEALTH', 'Mental Health', 'MENTAL_HEALTH', 'Mental Health', 1);

-- Conditions restricting mobility / dexterity
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'PHYSICAL_OTHER', 'Conditions restricting mobility / dexterity', 'MOBILITY', 'Conditions restricting mobility / dexterity', 11);

-- Neurological Condition
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'TOURETTES', 'Tourette''s Syndrome/Tic Disorder', 'NEUROLOGICAL_CONDITION', 'Neurological Condition', 12),
    (gen_random_uuid(), 'CONDITION', 'FASD', 'Foetal Alcohol Spectrum Disorder', 'NEUROLOGICAL_CONDITION', 'Neurological Condition', 13),
    (gen_random_uuid(), 'CONDITION', 'NEURO_OTHER', 'Other Neurological Condition', 'NEUROLOGICAL_CONDITION', 'Neurological Condition', 14);

-- Neurodegenerative condition
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'NEURODEGEN', 'Neurodegenerative condition', 'NEURODEGENERATIVE_CONDITION', 'Neurodegenerative condition', 15);

-- Long term Medical Condition
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'ABI', 'Acquired Brain Injury', 'LONG_TERM_MEDICAL_CONDITION', 'Long term Medical Condition', 16),
    (gen_random_uuid(), 'CONDITION', 'LONG_TERM_OTHER', 'Other Long term Medical Condition', 'LONG_TERM_MEDICAL_CONDITION', 'Long term Medical Condition', 17);

-- Other disabilities and health conditions
INSERT INTO reference_data (id, domain, code, description, category_code, category_description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'OTHER', 'Other Disability &/or health condition not listed above', 'OTHER', 'Other disabilities and health conditions', 18);
