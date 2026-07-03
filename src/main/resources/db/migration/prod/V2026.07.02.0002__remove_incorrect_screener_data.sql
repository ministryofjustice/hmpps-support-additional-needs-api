--- Remove screener data added in error from PRNs A0242CW, A0386EN, A1242AD, A2315FF, A2642DZ
--- Associated challenge and strength rows are removed via ON DELETE CASCADE
--- Jira ticket RR-2639

DELETE from aln_screener
    WHERE prison_number = 'A0242CW'
    AND reference = '3df91dcf-b94f-47f0-8ea7-78e58d2858f4';

DELETE from aln_screener
    WHERE prison_number = 'A0386EN'
    AND reference = '125f96fb-a11a-4011-a457-c7fe1a84d2fb';

DELETE from aln_screener
    WHERE prison_number = 'A1242AD'
    AND reference = '071ee44f-19e7-4795-903d-8d3a14272cef';

DELETE from aln_screener
    WHERE prison_number = 'A2315FF'
    AND reference = '82e98168-a421-4de0-b461-7d068e2df5fe';

DELETE from aln_screener
    WHERE prison_number = 'A2642DZ'
    AND reference = 'c77af385-42e4-4662-a5f3-965836f5b3ab';
