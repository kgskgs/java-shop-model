/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.models;

import shop.infrastructure.interfaces.Foreign;
import shop.infrastructure.interfaces.Key;
import shop.infrastructure.interfaces.Name;
import shop.infrastructure.interfaces.Table;
import shop.models.compare.ProdSortByCategory;

/**
 *
 * @author k
 */
@Table(Name = "products")
public class Product extends Model {
    @Key
    public int productId;
    @Name
    public String productName;
    public double price;
    @Foreign(comparator = ProdSortByCategory.class)
    public int categoryId;
    public int active;
    public String description;

    public Product(String productName, double price, int categoryId, int active, String description) {
        this.productName = productName;
        this.price = price;
        this.categoryId = categoryId;
        this.active = active;
        this.description = description;
    }

    public Product() {
    }
    
    
    
}


/*
CREATE TABLE products
(
    productId       INT unsigned NOT NULL AUTO_INCREMENT,
    productName     VARCHAR(150) NOT NULL,
    price           DECIMAL NOT NULL,
    categoryId      INT unsigned NOT NULL,
    active          TINYINT(1) unsigned NOT NULL, 
    description     TEXT NULL,
    PRIMARY KEY     (productId),
    FOREIGN KEY     (categoryId) REFERENCES productCategories(productCategoriyId)    
);
*/