/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
 */
package shop.models.compare;

import java.util.Comparator;
import shop.models.Product;

/**
 *
 * @author k
 */
public class ProdSortByCategory implements Comparator<Product>{

    @Override
    public int compare(Product o1, Product o2) {
        return o1.categoryId - o2.categoryId;
    }
    
}
