--- Remove screener data added in error from PRNs A1699EM and A1717FC
--- Jira ticket RR-2747

DELETE from aln_screener
    WHERE prison_number = 'A1699EM'
    AND reference = '9e49de37-3fc4-49c4-860d-78577e2b4bc0';

DELETE from aln_screener
    WHERE prison_number = 'A1717FC'
    AND reference = '52b2700d-c2b6-4c77-84e4-21b43b74f525';
