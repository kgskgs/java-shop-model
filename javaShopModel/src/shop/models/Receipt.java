/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.models;
import shop.infrastructure.interfaces.*;

/**
 *
 * @author k
 */
@Table(Name = "receipts")
public class Receipt extends Model{

    @Key
    public int receiptId;
    public int invoice;
    public int invoiceId;
    public int employeeId;
    @Timestamp
    public String buyDate;

    public Receipt(int invoice, int invoiceId, int employeeId, String buyDate) {
        this.invoice = invoice;
        this.invoiceId = invoiceId;
        this.employeeId = employeeId;
        this.buyDate = buyDate;
    }

    public Receipt() {
    }
    
}

/*
receiptId	int(10) UN AI PK
invoice	tinyint(1) UN
invoiceId	int(10) UN
employeeId	int(10) UN
buyDate	timestamp
*/