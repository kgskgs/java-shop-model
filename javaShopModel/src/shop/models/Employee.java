/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.models;

import shop.infrastructure.interfaces.Foreign;
import shop.infrastructure.interfaces.Key;
import shop.infrastructure.interfaces.Table;
import shop.models.compare.EmpSortByShop;

/**
 *
 * @author k
 */
@Table(Name = "employees")
public class Employee extends Model{
    @Key
    public int employeeId;
    public String username;
    public String firstname;
    public String lastname;
    public int accessLvl;
    public int active;
    @Foreign(comparator = EmpSortByShop.class)
    public int shopId;

    public Employee(String username, String firstname, String lastname, int accessLvl, int shopId) {
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.accessLvl = accessLvl;
        this.active = 1;
        this.shopId = shopId;
    }

    public Employee() {
    }
   
    
}


/*
employeeId	int(10) UN AI PK
username	varchar(32)
firstname	varchar(150)
lastname	varchar(150)
accessLvl	tinyint(1) UN
active	tinyint(1) UN
shopId	int(10) UN
*/