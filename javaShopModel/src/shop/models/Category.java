/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
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
    public int active;
    public String description;

    public Category(String categoryName, String description) {
        this.categoryName = categoryName;
        this.description = description;
        this.active = 1;
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