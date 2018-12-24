/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;

import javax.swing.JOptionPane;

/**
 *
 * @author k
 */
public class Dialogues {
    
    public static final String OVERWRITE_TXT = "This will overwrite the currently selected item.";
    public static final String DELETE_TXT = "This will delete the currently selected item\n"
            + "(it will still show up in reports, but will be able to be restored only from the database itself).";
    
    public static boolean confirmYesNo(String text){
        int response = JOptionPane.showConfirmDialog(null, text, "Confirm",
                                        JOptionPane.YES_NO_OPTION, 
                                        JOptionPane.QUESTION_MESSAGE);
        
        if (response == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }
}
