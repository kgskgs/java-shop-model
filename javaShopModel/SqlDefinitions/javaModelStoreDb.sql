/**
 * Author:  Lyuboslav
 * Created: Oct 23, 2018
 */
CREATE TABLE clients
(
    clientId    INT unsigned NOT NULL AUTO_INCREMENT,
    firstname   VARCHAR(150) NOT NULL,
    lastname    VARCHAR(150) NOT NULL,
    companyName VARCHAR(150) NOT NULL,
    bulstat     VARCHAR(15)  NOT NULL,
    PRIMARY     KEY(clientId) 
);

CREATE TABLE invoices
(
    invoiceId INT unsigned NOT NULL AUTO_INCREMENT,
    clientId  INT unsigned NOT NULL,
    orderDate TIMESTAMP NOT NULL,
    PRIMARY KEY (invoiceId),
    FOREIGN KEY (clientID) REFERENCES clients(clientId)    
);

CREATE TABLE deliverers
(
    delivererID INT unsigned NOT NULL AUTO_INCREMENT,
    firstname   VARCHAR(150) NOT NULL,
    lastname    VARCHAR(150) NOT NULL,
    bulstat     VARCHAR(15)  NOT NULL,
    phoneNumber VARCHAR(15)  NOT NULL,
    PRIMARY     KEY(delivererID) 
);

CREATE TABLE deliveries
(
    deliveryID    INT unsigned NOT NULL AUTO_INCREMENT,
    delivererID   INT unsigned NOT NULL,
    orderDate     TIMESTAMP NOT NULL,
    expectedDate  TIMESTAMP NOT NULL,
    deliveredDate TIMESTAMP NOT NULL,
    PRIMARY       KEY(deliveryID),
    FOREIGN KEY (delivererID) REFERENCES deliverers(delivererID)  
    
);

CREATE TABLE employees
(
    employeeId  INT unsigned NOT NULL AUTO_INCREMENT,
    firstname   VARCHAR(150) NOT NULL,
    lastname    VARCHAR(150) NOT NULL,
    PRIMARY KEY(employeeId) 
);

CREATE TABLE productCategories
(
    productCategoriyId  INT unsigned NOT NULL AUTO_INCREMENT,
    categoryName        VARCHAR(150) NOT NULL,
    PRIMARY KEY(productCategoriyId)
);

CREATE TABLE products
(
    productId   INT unsigned NOT NULL AUTO_INCREMENT,
    productName VARCHAR(150) NOT NULL,
    price       DECIMAL NOT NULL,
    categoryId  INT unsigned NOT NULL,
    PRIMARY KEY (productId),
    FOREIGN KEY (categoryId) REFERENCES productCategories(productCategoriyId)    
);

CREATE TABLE receipts
(
    receiptId       INT unsigned NOT NULL AUTO_INCREMENT, 
    invoice         INT NULL,              
    owner           VARCHAR(150) NOT NULL,               
    birth           DATE NOT NULL,            
    invoiceId       INT unsigned,
    employeeId      INT unsigned NOT NULL,
    PRIMARY KEY     (receiptId),
    FOREIGN KEY     (invoiceId) REFERENCES invoices(invoiceId)        
);

CREATE TABLE boughtProducts
(
    boughtProductId        INT unsigned NOT NULL AUTO_INCREMENT,
    productId              INT unsigned NOT NULL,
    receiptId              INT unsigned NOT NULL,
    PRIMARY KEY (boughtProductId),
    FOREIGN KEY (productId) REFERENCES products(productId),
    FOREIGN KEY (receiptId) REFERENCES receipts(receiptId)
);

CREATE TABLE deliveredProducts
(
    deliveredProductId     INT unsigned NOT NULL AUTO_INCREMENT, 
    productId              INT unsigned NOT NULL,
    deliveryId             INT unsigned NOT NULL, 
    PRIMARY KEY (deliveredProductId),
    FOREIGN KEY (productId)  REFERENCES products  (productId),
    FOREIGN KEY (deliveryId) REFERENCES deliveries(deliveryId)
);

