/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
 */
package shop.models;

import shop.infrastructure.interfaces.Key;
import shop.infrastructure.interfaces.Table;
import shop.infrastructure.*;

/**
 *
 * @author Lyuboslav
 */
@Table(Name = "clients")
public class Client extends Model {

    
    @Key
    public int clientId;
    public String eik;
    public String firstname;
    public String lastname;
    public String companyName;
    
    public Client(String eik, String firstname, String lastname, String companyName) {
        this.eik = eik;
        this.firstname = firstname;
        this.lastname = lastname;
        this.companyName = companyName;
    }
    
    public Client() {}
    

    
}

/*CREATE TABLE clients
(
    eik     	VARCHAR(15)  NOT NULL,
    firstname   VARCHAR(150) NOT NULL,
    lastname    VARCHAR(150) NOT NULL,
    companyName VARCHAR(150) NOT NULL,
    PRIMARY KEY(eik) 
);*/