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
    private volatile boolean forceCommit = false;
    
    public DBwriteThread(Connection con, BlockingQueue<String> q) throws SQLException{
        this.con = con;
        this.queryC = 0;
        
        setName("DBwriteThread");
        
        state = con.createStatement();
        queryStrQueue = q;
    }

    /**
     * wait for statements to be added to the queue
     * if the number of statements exceed BATCH_EXEC_SIZE or forceCommit flag is set,
     * write the statements to the database
     */
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " runs");
        while(true){
            try {
                    //tmpQueryStr = queryStrQueue.take(); //take blocks the thread
                    //poll used so we can force commit on the same thread
                    if ((tmpQueryStr = queryStrQueue.poll()) != null){
                        state.addBatch(tmpQueryStr);
                        queryC++;
                    }
                    if(queryC >= BATCH_EXEC_SIZE || (forceCommit && queryC > 0)){
                        state.executeBatch();
                        reportCommit();
                        queryC = 0;
                        forceCommit = false;
                    }
                    sleep(950);
            } 
            catch (SQLException ex) {
                System.out.println(ex.getMessage());
            } catch (InterruptedException ex) {
                System.out.println(ex.getMessage());
            } 
        }
    } 
        
    
    /**
     * used to write to DB regardless of batch size
     * @param fc boolean to set the force commit flag to
     */
    public void setForceCommit(boolean fc){
        forceCommit = fc;
    }
    
    private void reportCommit(){
        System.out.println(Thread.currentThread().getName() +": commiting " + queryC + " statements to database");
    }
    
}
