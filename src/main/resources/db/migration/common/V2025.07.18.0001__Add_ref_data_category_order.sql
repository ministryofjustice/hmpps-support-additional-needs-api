ALTER TABLE reference_data
    ADD COLUMN category_list_sequence INT NOT NULL DEFAULT 0;

update reference_data set category_list_sequence = 10 where category_code = 'LITERACY_SKILLS';
update reference_data set category_list_sequence = 20 where category_code = 'NUMERACY_SKILLS';
update reference_data set category_list_sequence = 30 where category_code = 'ATTENTION_ORGANISING_TIME';
update reference_data set category_list_sequence = 40 where category_code = 'LANGUAGE_COMM_SKILLS';
update reference_data set category_list_sequence = 50 where category_code = 'EMOTIONS_FEELINGS';
update reference_data set category_list_sequence = 60 where category_code = 'PHYSICAL_SKILLS';
update reference_data set category_list_sequence = 70 where category_code = 'SENSORY';
update reference_data set category_list_sequence = 90 where category_code = 'MEMORY';
update reference_data set category_list_sequence = 100 where category_code = 'PROCESSING_SPEED';


