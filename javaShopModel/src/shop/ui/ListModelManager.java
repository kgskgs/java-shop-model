/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import shop.infrastructure.interfaces.Foreign;
import shop.infrastructure.interfaces.Key;

/**
 * This will convert 1d array lists to 2d array lists of default list models
 * @author k
 * @param T model
 */
public class ListModelManager<T> {
    
    ArrayList<T> flatList;
    ArrayList<ArrayList<DefaultListModel<String>>> listModels;
    ArrayList<Integer> offsets;
    Class<T> modelClass;
    
    /**
     * new List Manager from a flat model list
     * @param flatList model to be split, sorted
     */
    public ListModelManager(Class<T> modelClass, ArrayList<T> flatList) {
        try {
            this.flatList = flatList;
            this.modelClass = modelClass;
            int separatorVal;
            Comparator comp = (Comparator) modelClass.getAnnotation(Foreign.class)
                    .comparator().getConstructor().newInstance();
            
            Collections.sort(flatList, comp);
            
            
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(ListModelManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ListModelManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(ListModelManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ListModelManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ListModelManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(ListModelManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    private int getForeignVal(T model){
        
        for (Field f : modelClass.getFields()) {
            if (f.isAnnotationPresent(Foreign.class)){
                try {
                    return f.getInt(model);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    return -1;
                }
            }
        }
        
        return -1;
    }
    
}
