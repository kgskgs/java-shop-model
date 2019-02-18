/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
 */
package shop.models;

import shop.infrastructure.interfaces.Key;
import shop.infrastructure.interfaces.Table;

/**
 *
 * @author Lyuboslav
 */
@Table(Name = "shops")
public class Shop extends Model{
    @Key
    public int shopId;
    public String shopName;
    public String address;
    public int active;

    public Shop(String shopName, String address) {
        this.shopName = shopName;
        this.address = address;
        active = 1;
    }

    public Shop() {
    }
    
    
}

/*CREATE TABLE shops
(
    shopId      INT unsigned NOT NULL AUTO_INCREMENT,
    shopName    VARCHAR(150) NOT NULL,
    address     VARCHAR(150) NOT NULL,
    PRIMARY KEY (shopName)
);*/