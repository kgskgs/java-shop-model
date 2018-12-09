/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.models;
import java.util.Date;
import shop.infrastructure.interfaces.Key;
import shop.infrastructure.interfaces.Table;
import shop.infrastructure.interfaces.Timestamp;

/**
 *
 * @author Lyuboslav
 */
@Table(Name = "invoices")
public class Invoice extends Model{
    @Key
    public int invoiceId;
    public int clientId;
    @Timestamp //in case we want automatic timestamp
    public String orderDate;

    public Invoice(int clientId, String orderDate) {
        this.clientId = clientId;
        this.orderDate = orderDate;
    }
    
    public Invoice() {}
}

/*
CREATE TABLE invoices
(
    invoiceId   INT unsigned NOT NULL AUTO_INCREMENT,
    clientId    INT unsigned NOT NULL,
    orderDate   TIMESTAMP,
    PRIMARY KEY (invoiceId),
    FOREIGN KEY (clientId) REFERENCES clients(clientId)    
);
*/