--- Remove screener data added in error from PRN A0060AF
--- Jira ticket RR-2514

DELETE from aln_screener
    WHERE prison_number = 'A0060AF'
    AND reference = '4eacab58-395d-469f-ab8a-e2635ff1710a';

DELETE from aln_screener
    WHERE prison_number = 'A0060AF'
    AND reference = 'b23461d8-31f2-46a3-a950-2185371f7547';
