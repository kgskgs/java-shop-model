/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.models.compare;

import java.util.Comparator;
import shop.models.Shop;

/**
 *
 * @author k
 */
public class ShopSortById  implements Comparator<Shop>{

    @Override
    public int compare(Shop o1, Shop o2) {
        return o1.shopId - o2.shopId;
    }
    
}
