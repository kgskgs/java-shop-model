/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import shop.models.Client;

/**
 *
 * @author Lyuboslav
 */
public class ClientRepository implements IRepository<Client> {
        
    private final Connection connection;
    private final Statement statement;
    
    public ClientRepository(Connection sqlConnection) throws SQLException
    {
        connection = sqlConnection;
        statement = connection.createStatement();        
    }
    
    @Override
    public ArrayList<Client> GetAll(int page, int count)
    {
        ArrayList<Client> result = new ArrayList<>();
        try {
            int offset = page * count;
            ResultSet set = statement.executeQuery("SELECT * FROM clients LIMIT " + count + " OFFSET " + offset + ";");
            while(set.next())
            {
                Client client = new Client();
                client.Eik = set.getString("eik");
                client.Firstname = set.getString("firstname");
                client.Lastname = set.getString("lastname");
                client.CompanyName = set.getString("companyName");
                result.add(client);
            }            
        } catch (SQLException ex) {
            Logger.getLogger(ClientRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    @Override
    public Client Get(int id)
    {
        try {          
            ResultSet set = statement.executeQuery("SELECT * FROM clients WHERE clientId = '" + id + "';");
            if (set.next()) {
                Client client = new Client();
                client.Eik = set.getString("eik");
                client.Firstname = set.getString("firstname");
                client.Lastname = set.getString("lastname");
                client.CompanyName = set.getString("companyName");
                return client;
            }
            else 
            {
                return null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientRepository.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    @Override
    public String Insert(Client c)
    {
        return "INSERT INTO clients VALUES (" + 
                c.Eik + "," +
                c.Firstname + "," +
                c.Lastname + "," +
                c.CompanyName + ");";
    }
    
    @Override
    public String Delete(Client c)
    {
        return "DELETE FROM clients WHERE eik = '" + c.Eik + "';";              
    }   
}