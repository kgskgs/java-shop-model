/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
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
