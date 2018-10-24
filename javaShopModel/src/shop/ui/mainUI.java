/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import shop.db.ConnectionFactory;
import shop.db.DatabaseDispatch;

/**
 *
 * @author K1
 */
public class mainUI {
    
    public static void main(String args[]) {
        
        BlockingQueue<String> queryStrQueueMaster = new LinkedBlockingQueue<String>();
     

        
        try {
            Connection con = ConnectionFactory.getConnection("jdbctest","javaShopModel","javaShopModel","3306");
            
                    
            DatabaseDispatch dbd = new DatabaseDispatch(con, queryStrQueueMaster);
            
            dbd.start();

            FormStart f1 = new FormStart(queryStrQueueMaster,dbd);

            java.awt.EventQueue.invokeLater(f1);

            System.out.println(Thread.currentThread().getName());
            
            f1.log("connected to DB");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        
        
    }
    
}
