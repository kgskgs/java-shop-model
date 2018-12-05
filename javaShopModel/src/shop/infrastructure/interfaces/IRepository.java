/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure.interfaces;

/**
 *
 * @author Lyuboslav
 */
public interface IRepository<T> {    
    public T[] GetAll(int page, int count);
    public T Get(int key);
    public void Insert(T model) throws IllegalAccessException;
    public void Update(T model);
    public void Delete(int id);
}
