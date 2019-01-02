INSERT INTO employees VALUES ( NULL, "emp1", "asd1", "dsa1", "0", "1", "1");
CREATE USER 'emp1'@'localhost' IDENTIFIED BY 'emp1';
GRANT 'shop_cashier' TO 'emp1'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp2", "asd2", "dsa2", "1", "1", "1");
CREATE USER 'emp2'@'localhost' IDENTIFIED BY 'emp2';
GRANT 'shop_cashier' TO 'emp2'@'localhost';
GRANT 'shop_manager' TO 'emp2'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp3", "asd3", "dsa3", "0", "1", "1");
CREATE USER 'emp3'@'localhost' IDENTIFIED BY 'emp3';
GRANT 'shop_cashier' TO 'emp3'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp4", "asd4", "dsa4", "1", "1", "2");
CREATE USER 'emp4'@'localhost' IDENTIFIED BY 'emp4';
GRANT 'shop_cashier' TO 'emp4'@'localhost';
GRANT 'shop_manager' TO 'emp4'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp5", "asd5", "dsa5", "0", "1", "2");
CREATE USER 'emp5'@'localhost' IDENTIFIED BY 'emp5';
GRANT 'shop_cashier' TO 'emp5'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp6", "asd6", "dsa6", "1", "1", "2");
CREATE USER 'emp6'@'localhost' IDENTIFIED BY 'emp6';
GRANT 'shop_cashier' TO 'emp6'@'localhost';
GRANT 'shop_manager' TO 'emp6'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp7", "asd7", "dsa7", "0", "1", "5");
CREATE USER 'emp7'@'localhost' IDENTIFIED BY 'emp7';
GRANT 'shop_cashier' TO 'emp7'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp8", "asd8", "dsa8", "1", "1", "5");
CREATE USER 'emp8'@'localhost' IDENTIFIED BY 'emp8';
GRANT 'shop_cashier' TO 'emp8'@'localhost';
GRANT 'shop_manager' TO 'emp8'@'localhost';

INSERT INTO employees VALUES ( NULL, "emp9", "asd9", "dsa9", "0", "1", "5");
CREATE USER 'emp9'@'localhost' IDENTIFIED BY 'emp9';
GRANT 'shop_cashier' TO 'emp9'@'localhost';

