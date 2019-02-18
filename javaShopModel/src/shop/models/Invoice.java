/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
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