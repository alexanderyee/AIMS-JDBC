sql queries

a) SELECT count(school_name) as total FROM aims2010 WHERE school_name LIKE '% High %' AND school_name NOT LIKE '% Junior %' AND school_name NOT LIKE '% Jr %' AND school_name NOT LIKE '% Jr. %'; 
b) SELECT count(school_name) AS total_charter FROM aims2010 WHERE is_charter = 'Y';
	SELECT count(school_name) AS total_charter_good FROM aims2010 WHERE is_charter = 'Y' AND math_pctFFB + math_pctA < math_pctP;
c) SELECT 