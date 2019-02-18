/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
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
