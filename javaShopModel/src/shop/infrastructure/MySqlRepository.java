/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure;

import shop.infrastructure.interfaces.Key;
import shop.infrastructure.interfaces.Table;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import shop.infrastructure.*;
import shop.infrastructure.interfaces.IRepository;

/**
 * @author Lyuboslav
 * @param <T> is db model
 */
public class MySqlRepository<T> implements IRepository<T> {
    
    protected Class<T> modelClass;
    private final Connection sqlConnection;
    private final BlockingQueue<String> sqlQueue;
    
    public MySqlRepository(Class<T> modelClass, Connection sqlConnection, BlockingQueue<String> sqlQueue){
        this.modelClass = modelClass;
        this.sqlConnection = sqlConnection;
        this.sqlQueue = sqlQueue;
    }
    
    protected T createFromCurrentLine(ResultSet set) throws SQLException{
        try {
            T modelObj = modelClass.newInstance();
            
            for (Field f : modelClass.getDeclaredFields() ) {
                String fieldName = f.getName();
                if (f.getType() == int.class) {
                    f.set(modelObj, set.getInt(fieldName));
                }
                else if (f.getType() == String.class) {
                    f.set(modelObj, set.getString(fieldName));
                }
                else if (f.getType() == double.class) {
                    f.set(modelObj, set.getString(fieldName));
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
    public T[] GetAll(int page, int count){
        try {
            //"SELECT * FROM {table} LIMIT " + count + " OFFSET " + offset + ";"
            int offset = page * count;
            StringBuilder sqlBuilder = new StringBuilder();
            
            sqlBuilder.append("Select FROM ")
                .append(modelClass.getName().toLowerCase())
                .append("s ")
                .append("LIMIT ")
                .append(count)
                .append(" OFFSET ")
                .append(offset)
                .append(";");
            
            ResultSet set = sqlConnection.createStatement().executeQuery(sqlBuilder.toString());
            
            ArrayList<T> resultList = new ArrayList<>();
            
            while(set.next())
            {
                resultList.add(createFromCurrentLine(set));
            }
            return (T[])resultList.toArray();
        } catch (SQLException ex) {
            Logger.getLogger(MySqlRepository.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    @Override
    public T Get(int id){       
        try {
            StringBuilder sqlBuilder = new StringBuilder();
            
            Field keyField = null;
            Field[] fields = modelClass.getDeclaredFields();
            
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
            
            sqlBuilder.append("Select FROM ")
                    .append(modelClass.getAnnotation(Table.class).Name())
                    .append(" WHERE ")
                    .append(keyField.getName())
                    .append(" = '")
                    .append(id)
                    .append("';");
            
            System.out.println(sqlBuilder.toString());
            
            ResultSet set = sqlConnection.createStatement().executeQuery(sqlBuilder.toString());
            
            set.next();
            
            return createFromCurrentLine(set);
        } catch (SQLException ex) {
            Logger.getLogger(MySqlRepository.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    @Override
    public void Insert(T model) throws IllegalAccessException{
        
            /*"INSERT INTO clients VALUES (" +
            c.Eik + "," +
            c.Firstname + "," +
            c.Lastname + "," +
            c.CompanyName + ");";*/
            
            StringBuilder sqlBuilder = new StringBuilder();
            
            sqlBuilder.append("INSERT INTO ")
                .append(modelClass.getAnnotation(Table.class).Name())
                .append(" VALUES { ");
            
            for (Field f : modelClass.getDeclaredFields() ){
                if (f.isAnnotationPresent(Key.class)) {
                    sqlBuilder.append("NULL,");
                }
                else{
                    sqlBuilder.append("\"")
                        .append(f.get(model).toString())
                        .append("\"")
                        .append(",");   
                }
            }
            sqlBuilder.deleteCharAt(sqlBuilder.length()).append(");");            
            sqlQueue.offer(sqlBuilder.toString());              
    }

    @Override
    public void Delete(int id) {
        // "DELETE FROM clients WHERE eik = '" + c.Eik + "';";
        StringBuilder sqlBuilder = new StringBuilder();
        Field keyField = null;
        Field[] fields = modelClass.getDeclaredFields();
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
                .append(id)
                .append(";");
        
        sqlQueue.offer(sqlBuilder.toString());
    }

    @Override
    public void Update(T model) {
        
    }
}
