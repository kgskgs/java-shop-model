INSERT INTO employees VALUES ( NULL, "emp1", "asd1", "dsa1", "0", "1", "1");
CREATE USER IF NOT EXISTS 'emp1'@'localhost' IDENTIFIED BY 'emp1';
GRANT INSERT, SELECT, UPDATE ON javashopmodeldb.* TO 'emp1'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp2", "asd2", "dsa2", "1", "1", "1");
CREATE USER IF NOT EXISTS 'emp2'@'localhost' IDENTIFIED BY 'emp2';
GRANT INSERT, SELECT, UPDATE ON javashopmodeldb.* TO 'emp2'@'localhost';
GRANT CREATE USER, GRANT OPTION ON *.* TO 'emp2'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp3", "asd3", "dsa3", "0", "1", "1");
CREATE USER IF NOT EXISTS 'emp3'@'localhost' IDENTIFIED BY 'emp3';
GRANT INSERT, SELECT, UPDATE ON javashopmodeldb.* TO 'emp3'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp4", "asd4", "dsa4", "1", "1", "2");
CREATE USER IF NOT EXISTS 'emp4'@'localhost' IDENTIFIED BY 'emp4';
GRANT INSERT, SELECT, UPDATE ON javashopmodeldb.* TO 'emp4'@'localhost';
GRANT CREATE USER, GRANT OPTION ON *.* TO 'emp4'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp5", "asd5", "dsa5", "0", "1", "2");
CREATE USER IF NOT EXISTS 'emp5'@'localhost' IDENTIFIED BY 'emp5';
GRANT INSERT, SELECT, UPDATE ON javashopmodeldb.* TO 'emp5'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp6", "asd6", "dsa6", "1", "1", "2");
CREATE USER IF NOT EXISTS 'emp6'@'localhost' IDENTIFIED BY 'emp6';
GRANT INSERT, SELECT, UPDATE ON javashopmodeldb.* TO 'emp6'@'localhost';
GRANT CREATE USER, GRANT OPTION ON *.* TO 'emp6'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp7", "asd7", "dsa7", "0", "1", "5");
CREATE USER IF NOT EXISTS 'emp7'@'localhost' IDENTIFIED BY 'emp7';
GRANT INSERT, SELECT, UPDATE ON javashopmodeldb.* TO 'emp7'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp8", "asd8", "dsa8", "1", "1", "5");
CREATE USER IF NOT EXISTS 'emp8'@'localhost' IDENTIFIED BY 'emp8';
GRANT INSERT, SELECT, UPDATE ON javashopmodeldb.* TO 'emp8'@'localhost';
GRANT CREATE USER, GRANT OPTION ON *.* TO 'emp8'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp9", "asd9", "dsa9", "0", "1", "5");
CREATE USER IF NOT EXISTS 'emp9'@'localhost' IDENTIFIED BY 'emp9';
GRANT INSERT, SELECT, UPDATE ON javashopmodeldb.* TO 'emp9'@'localhost';