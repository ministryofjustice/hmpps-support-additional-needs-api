--- Remove screener data added in error from PRN A6450AD
--- Jira ticket RR-2456

DELETE from aln_screener
    WHERE prison_number = 'A6450AD'
    AND reference = '0b5438f3-ee22-4b56-9fa2-cc8539aa2ea3';

DELETE from aln_screener
    WHERE prison_number = 'A6450AD'
    AND reference = '174cd9f7-0048-41cf-9a15-6c30c27a5886';
