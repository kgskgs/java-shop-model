/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
 */
package shop.models.compare;

import java.util.Comparator;
import shop.models.Employee;

/**
 *
 * @author k
 */
public class EmpSortByShop implements Comparator<Employee>{

    @Override
    public int compare(Employee o1, Employee o2) {
        return o1.shopId - o2.shopId;
    }
    
}
