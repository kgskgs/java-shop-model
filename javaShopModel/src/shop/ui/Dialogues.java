/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;

import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

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
    
    public static String passwordNew(){
        JPasswordField pass1 = new JPasswordField();
        JPasswordField pass2 = new JPasswordField();
        JPanel boxes = new JPanel(new GridLayout(2,2, 5, 10));
        boxes.add(new JLabel("password: ", SwingConstants.RIGHT));
        boxes.add(pass1);
        //boxes.add(Box.createHorizontalStrut(15));
        boxes.add(new JLabel("confirm: ", SwingConstants.RIGHT));
        boxes.add(pass2);
        
        int result = JOptionPane.showConfirmDialog(null, boxes, "New Password", 
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION){
            String p1 = String.valueOf(pass1.getPassword());
            String p2 = String.valueOf(pass2.getPassword());
            if (!p1.equals(p2))
                throw new java.lang.IllegalArgumentException("passwords don't match");
            return p1;
        }
        return null;
    }
}
