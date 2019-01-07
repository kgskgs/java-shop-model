/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * uses one function for all document change events; add a flag to switch functionality 
 * @author k
 */
public abstract class DocChangeListener implements DocumentListener {

    @Override
    public void insertUpdate(DocumentEvent e) {
        docChanged(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        docChanged(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        docChanged(e);
    }
    
    public abstract void docChanged(DocumentEvent e);
    
}
