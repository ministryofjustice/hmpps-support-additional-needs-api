--- Remove screener data added in error from PRNs A3890EZ, A6519FK, A8454AL, A8733EY
--- Jira ticket RR-2515

DELETE from aln_screener
    WHERE prison_number = 'A3890EZ'
    AND reference = 'a9ae74d6-029b-4a0c-b4ad-a4602d4795cb';

DELETE from aln_screener
    WHERE prison_number = 'A6519FK'
    AND reference = 'ececa23c-3fd8-47c5-995f-82b018df4204';

DELETE from aln_screener
    WHERE prison_number = 'A8454AL'
    AND reference = 'd536e8c5-d8bf-4c39-a0c9-e4812e38f3cd';

DELETE from aln_screener
    WHERE prison_number = 'A8733EY'
    AND reference = 'ac3d6511-0326-4b1b-958f-de3961eb9901';
