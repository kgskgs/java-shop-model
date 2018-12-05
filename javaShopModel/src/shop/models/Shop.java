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
 * @author Lyuboslav
 */
@Table(Name = "shops")
public class Shop {
    @Key
    public int shopId;
    public String shopName;
    public String address;
}

/*CREATE TABLE shops
(
    shopId      INT unsigned NOT NULL AUTO_INCREMENT,
    shopName    VARCHAR(150) NOT NULL,
    address     VARCHAR(150) NOT NULL,
    PRIMARY KEY (shopName)
);*/