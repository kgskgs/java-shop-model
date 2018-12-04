/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.models;
import java.util.Date;

/**
 *
 * @author Lyuboslav
 */
public class Invoice {
    public int invoiceId;
    public String shopEik;
    public Date orderDate;
}
/*
CREATE TABLE invoices
(
    invoiceId   INT unsigned NOT NULL AUTO_INCREMENT,
    eik    		VARCHAR(15) NULL,
    orderDate   TIMESTAMP NOT NULL,
    PRIMARY KEY (invoiceId),
    FOREIGN KEY (eik) REFERENCES clients(eik)    
);
*/