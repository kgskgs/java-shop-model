/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
 */
package shop.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;

/**
 * Thread used to insert/update in the database 
 * Also hold miscellaneous functions for database user administration
 * @author K1
 */
public class DBwriteThread extends Thread {
    
    private final Statement state;
    //execute batch after number of statements
    public static final int BATCH_EXEC_SIZE = 10;
    
    //# of strings added to the current batch
    private int queryC;
    private String tmpQueryStr;
    
    private final BlockingQueue<String> queryStrQueue;
    
    public static final String[] PRIVILEGES = {"INSERT, SELECT, UPDATE ON javashopmodeldb.*", "CREATE USER, GRANT OPTION ON *.*"};
    
    public DBwriteThread(Connection con, BlockingQueue<String> q) throws SQLException{
        this.queryC = 0;
        
        setName("DBwriteThread");
        
        state = con.createStatement();
        queryStrQueue = q;
    }

    /**
     * Wait for statements to be added to the queue.
     * If the number of statements exceed BATCH_EXEC_SIZE or forceCommit flag is set,
     * write the statements to the database
     */
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " runs");
        try{
            while(!"end".equals(tmpQueryStr = queryStrQueue.take())){
                try {
                        System.out.println(tmpQueryStr);
                        if (!"fc".equals(tmpQueryStr)){
                            state.addBatch(tmpQueryStr);
                            queryC++;
                        }

                        if(queryC >= BATCH_EXEC_SIZE || ("fc".equals(tmpQueryStr) && queryC>0)){ 
                            state.executeBatch();
                            reportCommit();
                            queryC = 0;
                        }
                } 
                catch (SQLException ex) {
                    System.err.println(ex.getSQLState());
                    System.out.println(ex.getMessage()); //ex.printStackTrace();
                }
            }
        } catch (InterruptedException ex) {
                System.out.println(ex.getMessage()); //ex.printStackTrace();
        } 
    }
    
    /**
     * inserts a single statement into the database and gets autogenerated key
     * this will run on the caller thread - only used when we need the key immediately 
     * @param sqlStr sql command to be executed
     * @param state Statement to execute the command
     * @return generated key or -1 on failure
     */
    public static int commitGetKey(String sqlStr, Statement state){
        int key = -1;
        ResultSet rs;
        
        System.out.println(sqlStr);
        
        try {
            System.out.println(Thread.currentThread().getName() +": commiting 1 statement to database");
            state.executeUpdate(sqlStr, Statement.RETURN_GENERATED_KEYS);
            
            rs = state.getGeneratedKeys();
            
            if (rs.next()) {
                key = rs.getInt(1);
            } else {
                System.out.println("failed getting generated key");
            }
            
            
        } catch (SQLException ex) {
            System.out.println(ex.getMessage()); //ex.printStackTrace();
        }
        
        return key;
    }
    
    /**
     * get a single int from the database by a unique column
     * @param field element to get
     * @param table to search in
     * @param key column to search by
     * @param keyVal value to search by
     * @return value found or -1 on failure
     */
    public int getUniqueInt (String field, String table, String key, String keyVal){
        try {
            String queryStr = "SELECT %s FROM %s WHERE %s='%s'";
            ResultSet rs = state.executeQuery(String.format(queryStr, field, table, key, keyVal));
            //go to first entry
            rs.next();
            return rs.getInt(1); //result sets are 1-indexed
        } catch (SQLException ex) {
            System.out.println(ex.getMessage()); //ex.printStackTrace();
            return -1;
        }
        
    }
    
    /**
     * prints # of statements written to database 
     */
    private void reportCommit(){
        System.out.println(Thread.currentThread().getName() +": commiting " + queryC + " statements to database");
    }
    
    /**
     * formats the sql command for new user
     * @param usr username
     * @param pass password
     * @return sql statement string
     */
    public static String getNewUserStr(String usr, String pass){
        return String.format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'", usr, pass);
    }
    
    /**
     * formats sql command for grating a role
     * @param usr username
     * @param rank rank (0 or 1)
     * @return sql statement string
     */
    public static String getGrantStr(String usr, int rank){
        if (rank != 0 && rank != 1)
            throw new java.lang.IllegalArgumentException("rank value must be 0 or 1");
        return String.format("GRANT %s TO '%s'@'localhost'", PRIVILEGES[rank], usr);
    }
    
    /**
     * formats sql command to drop a user
     * @param usr username
     * @return sql statement string
     */
    public static String getDropUserStr(String usr){
        return String.format("DROP USER '%s'@'localhost'", usr);
    }
    
    /**
     * formats sql command to revoke a role
     * @param usr username
     * @param rank rank to revoke (0 or 1)
     * @return sql statement string
     */
    public static String getRevokeStr(String usr, int rank){
        if (rank != 0 && rank != 1)
            throw new java.lang.IllegalArgumentException("rank value must be 0 or 1");
        return String.format("REVOKE %s FROM '%s'@'localhost'", PRIVILEGES[rank], usr);
    }
    
    /**
     * formats sql command to set new password
     * @param usr username
     * @param pass new password
     * @return  sql statement string
     */
    public static String getSetPasswordStr(String usr, String pass){
        return String.format("SET PASSWORD FOR '%s'@'localhost' = '%s'", usr, pass);
    }

}
