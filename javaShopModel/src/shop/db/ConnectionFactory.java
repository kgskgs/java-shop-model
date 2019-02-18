/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
 */
package shop.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 *
 * @author Lyuboslav
 */
public class ConnectionFactory {
    public static Connection getConnection(String dbName, String dbUser, String dbPass, String nPort) throws ClassNotFoundException, SQLException {		
        Connection con = null;
        String osname = System.getProperty("os.name");
        		
        //load the Driver Class
        //include driver in project libraries
        //windows: mysql-connector-java.XX
        //linux:   mysql.jar
        Class.forName("com.mysql.jdbc.Driver");
        
 
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