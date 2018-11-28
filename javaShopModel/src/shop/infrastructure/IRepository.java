/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure;

import java.util.ArrayList;

/**
 *
 * @author Lyuboslav
 * @param <T>
 */
public interface IRepository<T> {
    
    public ArrayList<T> GetAll(int page, int count);
    public T Get(int key);
    public String Insert(T model);
    public String Delete(T model);
}
