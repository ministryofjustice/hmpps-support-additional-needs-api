DELETE FROM reference_data WHERE domain = 'CHALLENGE';

INSERT INTO reference_data (
    id, domain, code, description,
    category_code, category_description,
    area_code, area_description,
    list_sequence
)
VALUES
    (gen_random_uuid(), 'CHALLENGE', 'READING', 'Reading', 'LITERACY_SKILLS', 'Literacy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 1),
    (gen_random_uuid(), 'CHALLENGE', 'SPELLING', 'Spelling', 'LITERACY_SKILLS', 'Literacy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 2),
    (gen_random_uuid(), 'CHALLENGE', 'WRITING', 'Writing', 'LITERACY_SKILLS', 'Literacy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 3),
    (gen_random_uuid(), 'CHALLENGE', 'ALPHABET_ORDERING', 'Alphabet ordering', 'LITERACY_SKILLS', 'Literacy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 4),
    (gen_random_uuid(), 'CHALLENGE', 'READING_COMPREHENSION', 'Reading comprehension', 'LITERACY_SKILLS', 'Literacy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 5),
    (gen_random_uuid(), 'CHALLENGE', 'READING_VISUAL_DISCRIMINATION', 'Reading (visual discrimination)', 'LITERACY_SKILLS', 'Literacy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 6),
    (gen_random_uuid(), 'CHALLENGE', 'TRACKING', 'Tracking', 'LITERACY_SKILLS', 'Literacy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 7),
    (gen_random_uuid(), 'CHALLENGE', 'LANGUAGE_DECODING', 'Language decoding', 'LITERACY_SKILLS', 'Literacy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 8),

    (gen_random_uuid(), 'CHALLENGE', 'SPEED_OF_CALCULATION', 'Speed of calculation', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 1),
    (gen_random_uuid(), 'CHALLENGE', 'ARITHMETIC', 'Arithmetic', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 2),
    (gen_random_uuid(), 'CHALLENGE', 'ESTIMATION', 'Estimation', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 3),
    (gen_random_uuid(), 'CHALLENGE', 'MATHS_LITERACY', 'Maths literacy', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 4),
    (gen_random_uuid(), 'CHALLENGE', 'MATHS_CONFIDENCE', 'Maths confidence', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 5),
    (gen_random_uuid(), 'CHALLENGE', 'FRACTIONS_PERCENTAGES', 'Fractions and percentages', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 6),
    (gen_random_uuid(), 'CHALLENGE', 'WORD_BASED_PROBLEMS', 'Word-based problems', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 7),
    (gen_random_uuid(), 'CHALLENGE', 'MONEY_MANAGEMENT', 'Money management', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 8),
    (gen_random_uuid(), 'CHALLENGE', 'NUMBER_RECALL', 'Number recall', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 9),
    (gen_random_uuid(), 'CHALLENGE', 'NUMBER_SEQUENCING', 'Number sequencing', 'NUMERACY_SKILLS', 'Numeracy Skills', 'COGNITION_LEARNING', 'Cognition & Learning', 10),

    (gen_random_uuid(), 'CHALLENGE', 'FOCUSING', 'Focusing', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 1),
    (gen_random_uuid(), 'CHALLENGE', 'TIDINESS', 'Tidiness', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 2),
    (gen_random_uuid(), 'CHALLENGE', 'FINISHING_TASKS', 'Finishing tasks', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 3),
    (gen_random_uuid(), 'CHALLENGE', 'PROBLEM_SOLVING', 'Problem solving', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 4),
    (gen_random_uuid(), 'CHALLENGE', 'TASK_INITIATION', 'Task initiation', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 5),
    (gen_random_uuid(), 'CHALLENGE', 'TIME_ALLOCATION', 'Time allocation/prioritisation', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 6),
    (gen_random_uuid(), 'CHALLENGE', 'SELF_ORGANISED', 'Self-organised (routine)', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 7),
    (gen_random_uuid(), 'CHALLENGE', 'FORWARD_PLANNING', 'Forward planning', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 8),
    (gen_random_uuid(), 'CHALLENGE', 'ATTENTION_TO_DETAIL', 'Attention to detail', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 9),
    (gen_random_uuid(), 'CHALLENGE', 'TASK_SWITCHING', 'Task switching', 'ATTENTION_ORGANISING_TIME', 'Attention, Organising & Time Management', 'COGNITION_LEARNING', 'Cognition & Learning', 10),

    (gen_random_uuid(), 'CHALLENGE', 'MEMORY', 'Memory', 'MEMORY', 'Memory', 'MEMORY', 'Memory', 1),
    (gen_random_uuid(), 'CHALLENGE', 'PROCESSING_SPEED', 'Processing speed', 'PROCESSING_SPEED', 'Processing speed', 'PROCESSING_SPEED', 'Processing speed', 1),

    (gen_random_uuid(), 'CHALLENGE', 'TURN_TAKING', 'Turn-taking', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 1),
    (gen_random_uuid(), 'CHALLENGE', 'LANGUAGE_FLUENCY', 'Language fluency', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 2),
    (gen_random_uuid(), 'CHALLENGE', 'SOCIAL_ADAPTABILITY', 'Social adaptability', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 3),
    (gen_random_uuid(), 'CHALLENGE', 'COMMUNICATION', 'Communication', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 4),
    (gen_random_uuid(), 'CHALLENGE', 'EXTROVERSION_INTROVERSION', 'Extroversion/Introversion', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 5),
    (gen_random_uuid(), 'CHALLENGE', 'SOCIAL_NUANCES', 'Social nuances', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 6),
    (gen_random_uuid(), 'CHALLENGE', 'SPEAKING', 'Speaking', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 7),
    (gen_random_uuid(), 'CHALLENGE', 'WORD_FINDING', 'Word finding', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 8),
    (gen_random_uuid(), 'CHALLENGE', 'ACTIVE_LISTENING', 'Active listening', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 9),
    (gen_random_uuid(), 'CHALLENGE', 'NON_VERBAL_COMMUNICATION', 'Non-verbal communication', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 10),
    (gen_random_uuid(), 'CHALLENGE', 'PEOPLE_PERSON', 'People person', 'LANGUAGE_COMM_SKILLS', 'Language & Communication skills', 'COMMUNICATION_INTERACTION', 'Communication & Interaction', 11),

-- Emotions & feelings
    (gen_random_uuid(), 'CHALLENGE', 'RESTFULNESS', 'Restfulness', 'EMOTIONS_FEELINGS', 'Emotions & feelings', 'SOCIAL_EMOTIONAL_MENTAL', 'Social, Emotional & Mental Health', 1),
    (gen_random_uuid(), 'CHALLENGE', 'READING_EMOTIONS', 'Reading emotions', 'EMOTIONS_FEELINGS', 'Emotions & feelings', 'SOCIAL_EMOTIONAL_MENTAL', 'Social, Emotional & Mental Health', 2),
    (gen_random_uuid(), 'CHALLENGE', 'EMPATHY', 'Empathy', 'EMOTIONS_FEELINGS', 'Emotions & feelings', 'SOCIAL_EMOTIONAL_MENTAL', 'Social, Emotional & Mental Health', 3),
    (gen_random_uuid(), 'CHALLENGE', 'IMPULSE_CONTROL', 'Impulse control', 'EMOTIONS_FEELINGS', 'Emotions & feelings', 'SOCIAL_EMOTIONAL_MENTAL', 'Social, Emotional & Mental Health', 4),
    (gen_random_uuid(), 'CHALLENGE', 'EMOTIONAL_CONTROL', 'Emotional control', 'EMOTIONS_FEELINGS', 'Emotions & feelings', 'SOCIAL_EMOTIONAL_MENTAL', 'Social, Emotional & Mental Health', 5),
    (gen_random_uuid(), 'CHALLENGE', 'MANAGING_CHANGE', 'Managing change (adaptability)', 'EMOTIONS_FEELINGS', 'Emotions & feelings', 'SOCIAL_EMOTIONAL_MENTAL', 'Social, Emotional & Mental Health', 6),
    (gen_random_uuid(), 'CHALLENGE', 'CONFIDENCE', 'Confidence', 'EMOTIONS_FEELINGS', 'Emotions & feelings', 'SOCIAL_EMOTIONAL_MENTAL', 'Social, Emotional & Mental Health', 7),
    (gen_random_uuid(), 'CHALLENGE', 'CALM', 'Calm', 'EMOTIONS_FEELINGS', 'Emotions & feelings', 'SOCIAL_EMOTIONAL_MENTAL', 'Social, Emotional & Mental Health', 8),

-- Physical Skills & coordination
    (gen_random_uuid(), 'CHALLENGE', 'HANDWRITING', 'Handwriting', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 1),
    (gen_random_uuid(), 'CHALLENGE', 'BALANCE', 'Balance', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 2),
    (gen_random_uuid(), 'CHALLENGE', 'FINE_MOTOR_SKILLS', 'Fine motor skills', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 3),
    (gen_random_uuid(), 'CHALLENGE', 'LEARNING_NEW_SKILLS', 'Learning new skills', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 4),
    (gen_random_uuid(), 'CHALLENGE', 'SPORTING_BALL_SKILLS', 'Sporting ball skills', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 5),
    (gen_random_uuid(), 'CHALLENGE', 'DUAL_TASKING', 'Dual tasking', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 6),
    (gen_random_uuid(), 'CHALLENGE', 'FOLDING_PACKING_SORTING', 'Folding, packing, and sorting', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 7),
    (gen_random_uuid(), 'CHALLENGE', 'STAMINA', 'Stamina', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 8),
    (gen_random_uuid(), 'CHALLENGE', 'GRASP', 'Grasp', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 9),
    (gen_random_uuid(), 'CHALLENGE', 'SPATIAL_AWARENESS', 'Spatial awareness', 'PHYSICAL_SKILLS', 'Physical Skills & coordination', 'PHYSICAL_SENSORY', 'Physical & Sensory', 10),

-- Sensory
    (gen_random_uuid(), 'CHALLENGE', 'AUDITORY_DISCRIMINATION', 'Auditory Discrimination', 'SENSORY', 'Sensory', 'PHYSICAL_SENSORY', 'Physical & Sensory', 1),
    (gen_random_uuid(), 'CHALLENGE', 'CREATIVITY', 'Creativity', 'SENSORY', 'Sensory', 'PHYSICAL_SENSORY', 'Physical & Sensory', 2),
    (gen_random_uuid(), 'CHALLENGE', 'LISTENING', 'Listening', 'SENSORY', 'Sensory', 'PHYSICAL_SENSORY', 'Physical & Sensory', 3),
    (gen_random_uuid(), 'CHALLENGE', 'SENSORY_PROCESSING', 'Sensory processing', 'SENSORY', 'Sensory', 'PHYSICAL_SENSORY', 'Physical & Sensory', 4),
    (gen_random_uuid(), 'CHALLENGE', 'VISUAL_SKILLS', 'Visual skills', 'SENSORY', 'Sensory', 'PHYSICAL_SENSORY', 'Physical & Sensory', 5),
    (gen_random_uuid(), 'CHALLENGE', 'VISUAL_SPATIAL_SKILLS', 'Visual-Spatial Skills', 'SENSORY', 'Sensory', 'PHYSICAL_SENSORY', 'Physical & Sensory', 6);

