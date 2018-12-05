DROP DATABASE IF EXISTS javashopmodeldb;

CREATE DATABASE javashopmodeldb;
use javashopmodeldb;

CREATE TABLE clients
(
    clientId    INT unsigned NOT NULL AUTO_INCREMENT,
    eik     	VARCHAR(15)  NOT NULL,
    firstname   VARCHAR(150) NOT NULL,
    lastname    VARCHAR(150) NOT NULL,
    companyName VARCHAR(150) NOT NULL,
    PRIMARY KEY(clientId) 
);

CREATE TABLE shops
(
    shopId      INT unsigned NOT NULL AUTO_INCREMENT,
    shopName    VARCHAR(150) NOT NULL,
    address     VARCHAR(150) NOT NULL,
    PRIMARY KEY (shopId)
);

CREATE TABLE invoices
(
    invoiceId   INT unsigned NOT NULL AUTO_INCREMENT,
    clientId    INT unsigned NOT NULL,
    orderDate   TIMESTAMP NOT NULL,
    PRIMARY KEY (invoiceId),
    FOREIGN KEY (clientId) REFERENCES clients(clientId)    
);

CREATE TABLE employees
(
	employeeId  INT unsigned NOT NULL AUTO_INCREMENT,
    username    VARCHAR(32), /*same as mysql username*/ 
    firstname   VARCHAR(150) NOT NULL,
    lastname    VARCHAR(150) NOT NULL,
    accessLvl   TINYINT(1) unsigned NOT NULL,
    active      TINYINT(1) unsigned NOT NULL, /*we can't drop people who are not working anymore*/ 
    shopId      INT unsigned NOT NULL,
    PRIMARY KEY (employeeId),
    FOREIGN KEY (shopId) REFERENCES shops(shopId)
);

CREATE TABLE productCategories
(
    productCategoriyId  INT unsigned NOT NULL AUTO_INCREMENT,
    categoryName        VARCHAR(150) NOT NULL,
    PRIMARY KEY         (productCategoriyId)
);

CREATE TABLE products
(
    productId       INT unsigned NOT NULL AUTO_INCREMENT,
    productName     VARCHAR(150) NOT NULL,
    price           DECIMAL NOT NULL,
    categoryId      INT unsigned NOT NULL,
    active          TINYINT(1) unsigned NOT NULL, /*we can't drop deleted products*/
    PRIMARY KEY     (productId),
    FOREIGN KEY     (categoryId) REFERENCES productCategories(productCategoriyId)    
);

CREATE TABLE receipts
(
    receiptId       INT unsigned NOT NULL AUTO_INCREMENT, 
    invoice         TINYINT(1) unsigned NOT NULL, 
    invoiceId       INT unsigned,
    employeeId      INT unsigned,
    buyDate         TIMESTAMP NOT NULL,
    PRIMARY KEY     (receiptId),
    FOREIGN KEY     (invoiceId)  REFERENCES invoices(invoiceId),
    FOREIGN KEY     (employeeId) REFERENCES employees(employeeId)
);

CREATE TABLE boughtProducts
(
    productId               INT unsigned NOT NULL,
    receiptId               INT unsigned NOT NULL,
    productCount            INT NOT NULL,
    currentPrice            DECIMAL NOT NULL,
    FOREIGN KEY (productId) REFERENCES products(productId),
    FOREIGN KEY (receiptId) REFERENCES receipts(receiptId)
);