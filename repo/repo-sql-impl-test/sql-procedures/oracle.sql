CREATE OR REPLACE
PROCEDURE cleanupTestDatabaseProc
IS
  TYPE vtab IS TABLE OF VARCHAR2(30);
  l_enabled_constraints     vtab := vtab();
  l_enabled_constraint_tabs vtab := vtab();
  i                         NUMBER := 0;
  j                         NUMBER := 0;
  BEGIN
-- drop activity tables
--     FOR cur IN (SELECT
--                   table_name
--                 FROM user_tables
--                 WHERE LOWER(table_name) LIKE 'act_%') LOOP
--     BEGIN
--       EXECUTE IMMEDIATE 'drop table ' || cur.table_name || ' CASCADE CONSTRAINTS ';
--       EXCEPTION
--       WHEN OTHERS THEN
--       DBMS_OUTPUT.put_line(cur.table_name || ' ' || SQLERRM);
--     END;
--     END LOOP;

-- disable FK constraints
    FOR cur IN (SELECT
                  table_name,
                  constraint_name
                FROM user_constraints
                WHERE status = 'ENABLED' AND constraint_type = 'R' AND LOWER(table_name) NOT LIKE 'act_%') LOOP
      i := i + 1;
      l_enabled_constraints.extend(1);
      l_enabled_constraint_tabs.extend(1);
      l_enabled_constraint_tabs(i) := cur.table_name;
      l_enabled_constraints(i) := cur.constraint_name;
    END LOOP;
    WHILE j < i LOOP
      j := j + 1;
      EXECUTE IMMEDIATE 'alter table ' || l_enabled_constraint_tabs(j) || ' disable constraint ' ||
                        l_enabled_constraints(j);
    END LOOP;

-- truncate (midpoint) or drop tables (activity)
    FOR cur IN (SELECT
                  table_name
                FROM user_tables WHERE LOWER(table_name) NOT LIKE 'act_%') LOOP
    BEGIN
      EXECUTE IMMEDIATE 'truncate table ' || cur.table_name;
      EXCEPTION
      WHEN OTHERS THEN
      DBMS_OUTPUT.put_line(cur.table_name || ' ' || SQLERRM);
    END;
    END LOOP;

-- enable FK constraints
    j := 0;
    WHILE j < i LOOP
      j := j + 1;
      EXECUTE IMMEDIATE 'alter table ' || l_enabled_constraint_tabs(j) || ' enable constraint ' ||
                        l_enabled_constraints(j);
    END LOOP;

  END;