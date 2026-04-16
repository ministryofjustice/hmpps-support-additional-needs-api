--- Remove screener data added in error from PRN A0490DW
--- Jira ticket RR-2513

DELETE from aln_screener
    WHERE prison_number = 'A0490DW'
    AND reference = '5894256e-8d68-41f9-8368-3c3c71c29afb';
