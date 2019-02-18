/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
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
