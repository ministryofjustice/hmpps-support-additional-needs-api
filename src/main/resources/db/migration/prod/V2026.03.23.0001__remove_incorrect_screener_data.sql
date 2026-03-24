--- Remove screener data added in error from PRN A4090CH
--- Jira ticket RR-2438

DELETE from aln_screener
    WHERE prison_number = 'A4090CH'
    AND reference = '5b41bfc9-fdfd-4b5b-b7f9-28a6d4f0bab5';
