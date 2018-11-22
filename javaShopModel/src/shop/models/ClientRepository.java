/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.models;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author Lyuboslav
 */
public class ClientRepository {
        
    private final Connection connection;
    private final Statement statement;
    private final BlockingQueue<String> writeQ;
    
    
    /**
    * new ClientRepository
    * @param sqlConnection connection to database for reading
    * @param queryStrQueue blocking queue to share data with the write thread
    */
    public ClientRepository(Connection sqlConnection, BlockingQueue<String> queryStrQueue) throws SQLException
    {
        connection = sqlConnection;
        //statement only used for reading
        statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);   
        writeQ = queryStrQueue;
    }
    
    /**
    * get all clients from DB
    * @param page ??
    * @param count ??
    */
    public ArrayList<Client> GetAll(int page, int count) throws SQLException
    {
        ArrayList<Client> result = new ArrayList<>();
        
        int offset = page * count;
        ResultSet set = statement.executeQuery("SELECT * FROM clients LIMIT " + count + " OFFSET " + offset);
        while(set.next())
        {
            Client client = new Client();
            client.clientId = set.getInt("clientId");
            client.Eik = set.getString("eik");
            client.Firstname = set.getString("firstname");
            client.Lastname = set.getString("lastname");
            client.CompanyName = set.getString("companyName");
            
            result.add(client);
        }            
        
        return result;
    }
    
    /**
    * get a single client from DB
    * @param eik key in DB
    */
    public Client Get(String eik) throws SQLException
    {        
        ResultSet set = statement.executeQuery("SELECT * FROM clients WHERE eik = '" + eik);
        if (set.next()) {
            Client client = new Client();
            client.clientId = set.getInt("clientId");
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
    }
    
    /**
    * insert a client into DB
    * @param c client to insert
    */
    public void Insert(Client c)
    {
        String insStmt = "INSERT INTO clients VALUES (" +
                "NULL," + //autoincrement needs NULL
                "\""+c.Eik + "\"," +
                "\""+c.Firstname + "\"," +
                "\""+c.Lastname + "\"," +
                "\""+c.CompanyName + "\")";
        
        writeQ.offer(insStmt);
    }
    
    /**
    * insert a client into DB
    * we always inset one client at a time so we can use the GUI textfields directly
    * @param Eik EIK string
    * @param Firstname cleint's first name string
    * @param Lastname cleint's last name string
    * @param CompanyName cleint's company name string
    */
    public void Insert(String Eik, String Firstname, String Lastname, String CompanyName)
    {
        String insStmt = "INSERT INTO clients VALUES (" + 
                "NULL," + //autoincrement needs NULL
                "\""+Eik + "\"," +
                "\""+Firstname + "\"," +
                "\""+Lastname + "\"," +
                "\""+CompanyName + "\")";
        
        writeQ.offer(insStmt);
    }
    
    /**
    * delete a client into DB
    * @param c client to delete
    */
    public void Delete(Client c)
    {
        String delStmt =  "DELETE FROM clients WHERE eik = '" + c.Eik;     
        
        writeQ.offer(delStmt);
    }
    
    /**
    * update a client in DB
    * @param c client to update
    */
    public void Update(Client c){
        String upStmt = "UPDATE clients SET " +
                "firstname = \"" + c.Firstname + "\"," +
                "lastname = \"" + c.Lastname + "\"," +
                "companyName = \"" + c.CompanyName + "\"," +
                "eik = \"" + c.Eik + "\"" +
                "WHERE clientId = " + c.clientId;

        writeQ.offer(upStmt);
    }
    
    /**
    * update a client in DB
    * same as insert with string params
    * @param clientId key for the client in db
    * @param Eik EIK string
    * @param Firstname cleint's first name string
    * @param Lastname cleint's last name string
    * @param CompanyName cleint's company name string
    */
    public void Update(int clientId, String Eik, String Firstname, String Lastname, String CompanyName){
        String upStmt = "UPDATE clients SET " +
                "firstname = \"" + Firstname + "\"," +
                "lastname = \"" + Lastname + "\"," +
                "companyName = \"" + CompanyName +  "\"," +
                "eik = \"" + Eik + "\"" +
                "WHERE clientId = " + clientId;

        writeQ.offer(upStmt);
    }
}
