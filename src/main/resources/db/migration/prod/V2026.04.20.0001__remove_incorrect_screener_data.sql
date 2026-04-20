--- Remove screener data added in error from PRNs A3890EZ, A6519FK, A8454AL, A8733EY
--- Jira ticket RR-2515

DELETE from aln_screener
    WHERE prison_number = 'A3890EZ'
    AND reference = '4cb9c3b5-a242-4d72-8429-f9a4cdd92507';

DELETE from aln_screener
    WHERE prison_number = 'A6519FK'
    AND reference = 'd0b649fc-340c-47c4-8dfb-6ce48ce58d3c';

DELETE from aln_screener
    WHERE prison_number = 'A8454AL'
    AND reference = '06cb5148-b5ca-47cf-8796-5f62fe972bc7';

DELETE from aln_screener
    WHERE prison_number = 'A8733EY'
    AND reference = 'c9479247-e60b-4043-b66a-1985f8bb7180';
