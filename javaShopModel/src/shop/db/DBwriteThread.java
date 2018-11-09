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
    private boolean forceCommit;
    
    private final BlockingQueue<String> queryStrQueue;
    
    public DBwriteThread(Connection con, BlockingQueue<String> q) throws SQLException{
        this.forceCommit = false;
        this.con = con;
        this.queryC = 0;
        
        state = con.createStatement();
        queryStrQueue = q;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " runs");
        try {
            while(true){
                
                try {
                    tmpQueryStr = queryStrQueue.take(); //take blocks the thread
                    state.addBatch(tmpQueryStr);
                    queryC++;
                    
                    if(queryC >= BATCH_EXEC_SIZE){
                        System.out.println(Thread.currentThread().getName() + " - commit to DB; " + forceCommit + " " + queryC); //TODO log
                        state.executeBatch();
                        queryC = 0;
                        forceCommit = false;
                    }
                    
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    public synchronized void setForceCommit(boolean fc){
        forceCommit = fc;
    }
    
    public void forceCommit(){
        try {
            System.out.println(Thread.currentThread().getName() + " force commit");
            state.executeBatch();
            queryC = 0;
        } catch (SQLException ex) {
             ex.printStackTrace();
        }
         
    }
    
}
