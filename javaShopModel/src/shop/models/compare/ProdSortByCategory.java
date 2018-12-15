/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
