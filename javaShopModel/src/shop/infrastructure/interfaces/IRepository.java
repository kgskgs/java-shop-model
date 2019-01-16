/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure.interfaces;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author Lyuboslav
 */
public interface IRepository<T> {   
    public ArrayList<T> GetAll (int page, int count, boolean active) throws SQLException;
    public T Get(int key)  throws SQLException;
    public void Insert(T model) throws IllegalAccessException;
    public int InsertGetKey(T model) throws SQLException;
    public void Update(T model);
    public void Delete(int id);
    //public int Count();
}
