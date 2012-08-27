-- If you encounter problems, you can set query logging on:
-- mysql -u root -p
-- set global general_log_file = '/var/tmp/mysql.log';
-- set global general_log = 1;
-- Do not forget to reset the logging after tests!!! by using
-- set global general_log = 0;
-- Administrative user for midPoint
CREATE USER 'midpointadmin'@'%' IDENTIFIED BY 'secret';
GRANT ALL on *.* TO 'midpointadmin'@'%' WITH GRANT OPTION;

-- Sample prototype user used as User Model in connector
-- This sample user has global SELECT privilege
CREATE USER 'prototypeuser'@'%' IDENTIFIED BY 'secret';
GRANT SELECT on *.* TO 'prototypeuser'@'%';
