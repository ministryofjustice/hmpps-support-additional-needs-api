--- Remove screener data added in error from PRN A6368DF
--- Service desk incident INC4157097

DELETE from aln_screener
    WHERE prison_number = 'A6368DF'
    AND reference = '5338aa49-9c37-4189-ad62-cc092ffb015f';
