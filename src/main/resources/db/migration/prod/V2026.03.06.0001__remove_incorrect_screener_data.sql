--- Remove screener data added in error from PRN A2429FK

DELETE from aln_screener
    WHERE prison_number = 'A2429FK'
    AND reference = '652ec975-273f-480a-b183-a5c0259df122';

DELETE from aln_screener
WHERE prison_number = 'A2429FK'
  AND reference = 'f8229eb5-0964-4209-9ceb-6a0efc9610c3';

