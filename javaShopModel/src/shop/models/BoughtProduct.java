/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
 */
package shop.models;

import shop.infrastructure.interfaces.Table;

/**
 *
 * @author k
 */
@Table(Name = "boughtProducts")
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