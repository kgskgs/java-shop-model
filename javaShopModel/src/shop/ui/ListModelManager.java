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
import shop.infrastructure.interfaces.Name;


/**
 * This will convert 1d array lists to 2d array lists of default list models
 * @author k
 * @param T model
 */
public class ListModelManager<T> {
    
    ArrayList<DefaultListModel<String>> listModels;
    ArrayList<ArrayList<Integer>> flatIndeces;
    Class<T> modelClass;
    int selectedModel = -1;
    
    /**
     * new List Manager from a flat model list
     * @param modelClass the class of the models stored
     * @param flatList model to be split, sorted
     * @param parentList keys of all existing parent categories to split the flat list by
     */
    public ListModelManager(Class<T> modelClass, ArrayList<T> flatList, ArrayList<Integer> parentList) {
        try {
            this.modelClass = modelClass;
            flatIndeces = new ArrayList<>();
            listModels = new ArrayList<>();
                    
            int separatorVal = -1; 
            int tmpSepVal;
            
            DefaultListModel<String> tmpModel = null;
            ArrayList<Integer> tmpIndeces = null;
            int currentIndex = 0; 
            
            Collections.sort(flatList, getComparator());
            
            for (T model: flatList){
                tmpSepVal = getForeignVal(model);
                if (separatorVal != tmpSepVal){ //new category
                    if (tmpModel != null){
                        listModels.add(tmpModel);
                        flatIndeces.add(tmpIndeces);
                    }
                    
                    separatorVal  = tmpSepVal;
                    
                    tmpIndeces = new ArrayList<>();
                    tmpIndeces.add(currentIndex);
                    
                    tmpModel = new DefaultListModel<>();
                    tmpModel.addElement("New");
                    
                    tmpModel.addElement(Integer.toString(getKey(model)) + ": " 
                                        + getName(model));
                } else {
                   tmpModel.addElement(Integer.toString(getKey(model)) + ": " 
                                        + getName(model)); 
                }               

                currentIndex++; 
            }
            

           
        } catch (SecurityException ex) {
            Logger.getLogger(ListModelManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
           Logger.getLogger(ListModelManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
           Logger.getLogger(ListModelManager.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    /**
     * 
     * @param index 
     * @return DefaultListModel at index from listModels
     */
    public DefaultListModel<String> getModel(int index){
        selectedModel = index;
        return listModels.get(index);
    }
    
    /**
     * gets the flat (original) index of an item selected from a DefaultListModel
     * @param selected index of the selected item
     * @return 
     */
    public int getFlatIndex(int selected){
        return flatIndeces.get(selectedModel).get(selected);
    }
    
    /**
     * get the value of the foreign key of a model
     * @param model whose foreign key value to get
     * @return the value of the field annotated with @Foreign; -1 of failure
     */
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
    
    /**
     * Get a comparator for the model class
     * @return Comparator instanciated from the comparator field of the @Foreign field
     * @throws InstantiationException
     * @throws IllegalAccessException 
     */
    private Comparator getComparator() throws InstantiationException, IllegalAccessException{
        Class compClass;
        
        for (Field f : modelClass.getFields()) {
            if (f.isAnnotationPresent(Foreign.class)){
                compClass = f.getAnnotation(Foreign.class).comparator();
                return (Comparator)compClass.newInstance();
            }
        }
        
        return null;
    }
    
    private String getName(T model){
        for (Field f : modelClass.getFields()) {
            if (f.isAnnotationPresent(Name.class)){
                try {
                    return (String) f.get(model);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    return null;
                }
            }
        }
        return null;
    }
    
    /**
     * get the value of the key of a model
     * @param model whose key value to get
     * @return the value of the field annotated with @Key; -1 of failure
     */    
    private int getKey(T model){
        for (Field f : modelClass.getFields()) {
            if (f.isAnnotationPresent(Key.class)){
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
