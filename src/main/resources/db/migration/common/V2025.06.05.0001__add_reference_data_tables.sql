CREATE TABLE reference_data
(
    id                  UUID                  PRIMARY KEY NOT NULL,
    domain              VARCHAR(30)           NOT NULL,
    code                VARCHAR(60)           NOT NULL,
    description         VARCHAR(1000)         NOT NULL,
    category_code        VARCHAR(100)          ,
    category_description VARCHAR(1000)         ,
    area_code            VARCHAR(100)          ,
    area_description     VARCHAR(1000)         ,
    list_sequence       INT                   ,
    deactivated_at      TIMESTAMP             ,
    UNIQUE(domain, code)
);

create index idx_reference_data_domain
    on reference_data (domain);

create index idx_reference_data_code
    on reference_data (code);


INSERT INTO reference_data (id, domain, code, description, list_sequence)
VALUES
    (gen_random_uuid(), 'CONDITION', 'ABI', 'Acquired Brain Injury', 1),
    (gen_random_uuid(), 'CONDITION', 'ASC', 'Autism Spectrum Condition', 2),
    (gen_random_uuid(), 'CONDITION', 'ADHD', 'Attention Deficit Hyperactivity Disorder', 3),
    (gen_random_uuid(), 'CONDITION', 'DYSLEXIA', 'Dyslexia', 4),
    (gen_random_uuid(), 'CONDITION', 'DYSPRAXIA', 'Dyspraxia/Developmental Coordination Disorder', 5),
    (gen_random_uuid(), 'CONDITION', 'DYSCALCULIA', 'Dyscalculia', 6),
    (gen_random_uuid(), 'CONDITION', 'DYSGRAPHIA', 'Dysgraphia', 7),
    (gen_random_uuid(), 'CONDITION', 'DLD', 'Developmental Language Disorder', 8),
    (gen_random_uuid(), 'CONDITION', 'FASD', 'Foetal Alcohol Spectrum disorders', 9),
    (gen_random_uuid(), 'CONDITION', 'LD', 'Learning Disability', 10),
    (gen_random_uuid(), 'CONDITION', 'NEURODEGEN', 'Neurodegenerative condition (e.g. dementia, Alzheimer’s)', 11),
    (gen_random_uuid(), 'CONDITION', 'SENSORY_IMPAIR', 'Sensory impairment (e.g. visual or hearing impairment)', 12),
    (gen_random_uuid(), 'CONDITION', 'TOURETTES', 'Tourette’s Syndrome/Tic Disorder', 13),
    (gen_random_uuid(), 'CONDITION', 'MENTAL_HEALTH', 'Other - Mental Health condition (Bi-polar, personality disorder, depression)', 14),
    (gen_random_uuid(), 'CONDITION', 'PHYSICAL_OTHER', 'Other - Physical condition', 15),
    (gen_random_uuid(), 'CONDITION', 'NEURO_OTHER', 'Other Neurological / Neurodevelopmental condition (examples could include Epilepsy, Cerebral palsy, etc)', 16),
    (gen_random_uuid(), 'CONDITION', 'LONG_TERM_MED', 'Long term Medical Condition e.g. Cerebral Palsy, Diabetes, Spina Bifida', 17),
    (gen_random_uuid(), 'CONDITION', 'OTHER', 'Other disabilities and health conditions', 18);
