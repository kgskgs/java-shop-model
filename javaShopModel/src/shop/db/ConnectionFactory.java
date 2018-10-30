/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
/**
 *
 * @author Lyuboslav
 */
public class ConnectionFactory {
    public static Connection getConnection(String dbName, String dbUser, String dbPass, String nPort) throws ClassNotFoundException, SQLException {		
        Connection con = null;
        		
        //load the Driver Class
        //include ConnectorJ jar in Libraries
        Class.forName("com.mysql.cj.jdbc.Driver");
                
        String conUrl = String.format("jdbc:mysql://localhost:%s/%s?"   //database url + port
                + "verifyServerCertificate=false&useSSL=true&rewriteBatchedStatements=true", //connection options
                nPort, dbName);

        // create the connection now
        con = DriverManager.getConnection(conUrl,dbUser,dbPass);
        
	return con;
    }
}


/*
}catch (ClassNotFoundException e) {
            //connector library missing
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }    
    
*/