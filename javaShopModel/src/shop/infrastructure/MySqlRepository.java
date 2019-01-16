/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure;

import shop.infrastructure.interfaces.*;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import shop.db.DBwriteThread;


/**
 * @author Lyuboslav
 * @param <T> is db model
 */
public class MySqlRepository<T> implements IRepository<T> {
    
    protected Class<T> modelClass;
    //private final Connection sqlConnection;
    private final BlockingQueue<String> sqlQueue;
    
    private Statement state; //"By default, only one ResultSet object per Statement object can be open at the same time."
    
    public MySqlRepository(Class<T> modelClass, Connection sqlConnection, BlockingQueue<String> sqlQueue) throws SQLException{
        this.modelClass = modelClass;
        //this.sqlConnection = sqlConnection;
        this.sqlQueue = sqlQueue;
        
        state = sqlConnection.createStatement();
    }
    
    protected T createFromCurrentLine(ResultSet set) throws SQLException{
        try {
            T modelObj = modelClass.newInstance();
            
            //getFields returns all **public** fields
            for (Field f : modelClass.getFields() ) {
                String fieldName = f.getName();
                if (f.getType() == int.class || f.getType() == Integer.class) {
                    f.set(modelObj, set.getInt(fieldName));
                }
                else if (f.getType() == String.class) {
                    f.set(modelObj, set.getString(fieldName));
                }
                else if (f.getType() == double.class) {
                    f.set(modelObj, set.getDouble(fieldName));
                }
                else if (f.getType() == Date.class) {
                    f.set(modelObj, set.getDate(fieldName));
                }
            }
            
            return modelObj;
        } catch (InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(MySqlRepository.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    @Override
    public ArrayList<T> GetAll(int page, int count, boolean active) throws SQLException{
        //"SELECT * FROM {table} LIMIT " + count + " OFFSET " + offset + ";"
        int offset = page * count;
        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("SELECT * FROM ")
            .append(modelClass.getAnnotation(Table.class).Name());
        
        if (active) 
            sqlBuilder.append(" WHERE active = 1");  
            
        sqlBuilder.append(" LIMIT ")
            .append(count)
            .append(" OFFSET ")
            .append(offset);

        //System.out.println(sqlBuilder.toString());
        ResultSet set = state.executeQuery(sqlBuilder.toString());

        ArrayList<T> resultList = new ArrayList<>();

        while(set.next())
        {
            resultList.add(createFromCurrentLine(set));
        }
        return resultList;
    }
    
    @Override
    public T Get(int id)  throws SQLException{       
        
        StringBuilder sqlBuilder = new StringBuilder();

        Field keyField = null;
        Field[] fields = modelClass.getFields();

        for (Field f : fields ) {
            if (f.isAnnotationPresent(Key.class)) {
                keyField = f;
                break;
            }
        }

        if (keyField == null) {
            System.out.println("Primary key not set for " + modelClass.getName());
            return null;
        }

        sqlBuilder.append("Select * FROM ")
                .append(modelClass.getAnnotation(Table.class).Name())
                .append(" WHERE ")
                .append(keyField.getName())
                .append(" = '")
                .append(id)
                .append("'");

        //System.out.println(sqlBuilder.toString());
        System.out.println(sqlBuilder.toString());
        ResultSet set = state.executeQuery(sqlBuilder.toString());

        set.next();

        return createFromCurrentLine(set);
    }
    
    private String buildInsertString(T model) throws IllegalArgumentException, IllegalAccessException{
        /*"INSERT INTO clients VALUES (" +
        c.Eik + "," +
        c.Firstname + "," +
        c.Lastname + "," +
        c.CompanyName + ");";*/
                
        StringBuilder sqlBuilder = new StringBuilder();
        String tmpString;

        sqlBuilder.append("INSERT INTO ")
            .append(modelClass.getAnnotation(Table.class).Name())
            .append(" VALUES ( ");

        for (Field f : modelClass.getFields() ){
            if (f.isAnnotationPresent(Key.class)) { // || f.isAnnotationPresent(Timestamp.class) - for autogenerated
                sqlBuilder.append("NULL,");
            }
            else{
                    tmpString = (f.get(model) == null)? "NULL" : "'"+f.get(model).toString()+"'";
                    sqlBuilder.append(tmpString)
                            .append(",");

            }
        }
        sqlBuilder.deleteCharAt(sqlBuilder.length()-1).append(")");

        return sqlBuilder.toString();
    }
    
    @Override
    public void Insert(T model) {
        String sqlStr;

        try {
            sqlStr = buildInsertString(model);
            sqlQueue.offer(sqlStr);     
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }
   
    @Override
    public int InsertGetKey(T model) {
        String sqlStr;
        int key = -1;

        try {
            sqlStr = buildInsertString(model);
            key = DBwriteThread.commitGetKey(sqlStr, state);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            ex.printStackTrace();
        } 
        return key;
    }
    
    @Override
    public void Delete(int id) {
        // "DELETE FROM clients WHERE eik = '" + c.Eik + "';";
        StringBuilder sqlBuilder = new StringBuilder();
        Field keyField = null;
        Field[] fields = modelClass.getFields();
        for (Field f : fields ) {
            if (f.isAnnotationPresent(Key.class)) {
                keyField = f;
                break;
            }
        }
        if (keyField == null) {
            System.out.println("Primary key not set for " + modelClass.getName());
            return;
        }
        
        sqlBuilder.append("DELETE FROM ")
                .append(modelClass.getAnnotation(Table.class).Name())
                .append(" WHERE ")
                .append(keyField.getName())
                .append(" = ")
                .append(id);
        
        sqlQueue.offer(sqlBuilder.toString());
    }

    @Override
    public void Update(T model) {
        try {
            StringBuilder sqlBuilder = new StringBuilder();
            Field keyField = null;
            Field[] fields = modelClass.getFields();
            
            sqlBuilder.append("UPDATE ")
                    .append(modelClass.getAnnotation(Table.class).Name())
                    .append(" SET ");
            
            for (Field f : fields ) {
                if (f.isAnnotationPresent(Key.class)) {
                    keyField = f;
                } else {
                    sqlBuilder.append(f.getName())
                            .append(" = \"")
                            .append(f.get(model).toString())
                            .append("\",");
                }
            }
            
            sqlBuilder.deleteCharAt(sqlBuilder.length()-1)  //remove last ','
                    .append(" WHERE ")
                    .append(keyField.getName())
                    .append(" = ")
                    .append(keyField.get(model).toString());
            
            //System.out.println(sqlBuilder.toString());
            sqlQueue.offer(sqlBuilder.toString());
            
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
        
    }

//    @Override
//    public int Count() {
//        int result = 0;
//        
//        StringBuilder sqlBuilder = new StringBuilder();
//        sqlBuilder.append("SELECT COUNT(*) FROM ")
//            .append(modelClass.getAnnotation(Table.class).Name());
//        try {
//            ResultSet set = state.executeQuery(sqlBuilder.toString());            
//            result = set.getInt("COUNT(*)");
//        } catch (SQLException ex) {
//            Logger.getLogger(MySqlRepository.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return result;                  
//    }
}
