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
    public static Connection getConnection() {		
		Connection con = null;
		try {
			
			// load the Driver Class
			Class.forName("shopUI.mainUI");

			// create the connection now
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/UserDB?rewriteBatchedStatements=true",
					"root",
					"javaShopModel");
		}catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return con;
	}
}
