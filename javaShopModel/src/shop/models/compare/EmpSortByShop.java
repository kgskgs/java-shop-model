/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
