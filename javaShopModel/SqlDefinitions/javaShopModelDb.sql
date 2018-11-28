DROP DATABASE IF EXISTS javashopmodeldb;

CREATE DATABASE javashopmodeldb;
use javashopmodeldb;

CREATE TABLE clients
(
    eik     	VARCHAR(15)  NOT NULL,
    firstname   VARCHAR(150) NOT NULL,
    lastname    VARCHAR(150) NOT NULL,
    companyName VARCHAR(150) NOT NULL,
    PRIMARY KEY(eik) 
);

CREATE TABLE shops
(
    shopId      INT unsigned NOT NULL AUTO_INCREMENT,
    shopName    VARCHAR(150) NOT NULL,
    address     VARCHAR(150) NOT NULL,
    PRIMARY KEY (shopName)
);

CREATE TABLE invoices
(
    invoiceId   INT unsigned NOT NULL AUTO_INCREMENT,
    eik    		VARCHAR(15) NULL,
    orderDate   TIMESTAMP NOT NULL,
    PRIMARY KEY (invoiceId),
    FOREIGN KEY (eik) REFERENCES clients(eik)    
);

CREATE TABLE employees
(
    employeeId  VARCHAR(32), /*same as mysql username*/ 
    firstname   VARCHAR(150) NOT NULL,
    lastname    VARCHAR(150) NOT NULL,
    accessLvl   TINYINT(1) unsigned NOT NULL,
    active      TINYINT(1) unsigned NOT NULL, /*we can't drop people who are not working anymore*/ 
    shopName    VARCHAR(150) NOT NULL,
    PRIMARY KEY (employeeId),
    FOREIGN KEY (shopName) REFERENCES shops(shopName)
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
    employeeId      VARCHAR(32),
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