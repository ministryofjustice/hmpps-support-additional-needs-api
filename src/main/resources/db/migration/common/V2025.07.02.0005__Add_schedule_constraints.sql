create unique index review_schedule_one_scheduled_per_prison_number_idx
on review_schedule (prison_number)
    where status = 'SCHEDULED';

create unique index plan_creation_schedule_one_scheduled_per_prison_number_idx
    on plan_creation_schedule (prison_number)
    where status = 'SCHEDULED';
