/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import shop.infrastructure.interfaces.Repository;
import shop.models.Client;

/**
 *
 * @author Lyuboslav
 */
public class ClientRepository extends Repository<Client> {
    
    public ClientRepository(Connection sqlConnection) throws SQLException
    {
        super(Client.class, sqlConnection);  
    }    
    
    @Override
    public Client[] GetAll(int page, int count)
    {        
        return getAllAndBind(page, count);
    }
    
    @Override
    public Client Get(int id)
    {
        return getAndBind(id);
    }
    
    @Override
    public void Insert(Client c)
    {
        executeInsert(c);
    } 
    
    @Override
    public void Delete(Client c)
    {
        executeDelete(c.clientId);
    }   
}
