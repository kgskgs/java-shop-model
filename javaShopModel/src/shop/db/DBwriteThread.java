/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author K1
 */
public class DBwriteThread extends Thread {
    
    //connection to database
    private Connection con;
    private Statement state;
    //execute batch after number of statements
    public static final int BATCH_EXEC_SIZE = 10;
    
    //
    private int queryC;
    private String tmpQueryStr;
    
    private final BlockingQueue<String> queryStrQueue;
    
    public DBwriteThread(Connection con, BlockingQueue<String> q) throws SQLException{
        this.con = con;
        this.queryC = 0;
        
        setName("DBwriteThread");
        
        state = con.createStatement();
        queryStrQueue = q;
    }

    /**
     * wait for statements to be added to the queue
     * if the number of statements exceed BATCH_EXEC_SIZE,
     * write the statements to the database
     */
    @Override
    public void run() {
        System.out.print(Thread.currentThread().getName() + " runs");
        try {
            while(true){
                try {
                    tmpQueryStr = queryStrQueue.take(); //take blocks the thread
                    state.addBatch(tmpQueryStr);
                    queryC++;
                    
                    if(queryC >= BATCH_EXEC_SIZE){
                        reportCommit();
                        state.executeBatch();
                        queryC = 0;
                    }
                } catch (SQLException ex) {
                    System.out.print(ex.getMessage());
                }
            }
        } catch (InterruptedException ex) {
            System.out.print(ex.getMessage());
        }
    }
     
    /**
     * write to DB regardless of batch size
     */
    public void forceCommit(){
        if(queryC > 0){
            try {
                reportCommit();
                state.executeBatch();
                queryC = 0;
            } catch (SQLException ex) {
                System.out.print(ex.getMessage());
            }
        } else {
            System.out.print("nothing to commit");
        }
    }
    
    private void reportCommit(){
        System.out.print("commiting " + queryC + " statements to database");
    }
    
}
