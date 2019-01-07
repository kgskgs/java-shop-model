/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.models;

import shop.infrastructure.interfaces.Table;

/**
 *
 * @author k
 */
@Table(Name = "boughtproducts")
public class BoughtProduct extends Model{
    public int productId;
    public int receiptId;
    public int productCount;
    public double currentPrice;

    public BoughtProduct(int productId, int receiptId, int productCount, double currentPrice) {
        this.productId = productId;
        this.receiptId = receiptId;
        this.productCount = productCount;
        this.currentPrice = currentPrice;
    }

    public BoughtProduct() {
    }
    
    
}

/*
productId int(10) UN 
receiptId int(10) UN 
productCount int(11) 
currentPrice decimal(13,2)
*/