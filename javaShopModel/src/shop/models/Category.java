/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.models;

import shop.infrastructure.interfaces.Key;
import shop.infrastructure.interfaces.Table;

/**
 *
 * @author k
 */
@Table(Name = "productCategories")
public class Category extends Model {
    @Key
    public int productCategoriyId;
    public String categoryName;
    public String description;

    public Category(String categoryName, String description) {
        this.categoryName = categoryName;
        this.description = description;
    }

    public Category() {
    }
    
    
}


/*
CREATE TABLE productCategories
(
    productCategoriyId  INT unsigned NOT NULL AUTO_INCREMENT,
    categoryName        VARCHAR(150) NOT NULL,
    description         TEXT NULL,
    PRIMARY KEY         (productCategoriyId)
);
*/