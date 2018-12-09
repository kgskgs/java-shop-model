/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import shop.models.Client;

/**
 *
 * @author K1
 */
public class mainUI {
    
    public static void main(String args[]) {
       
       //set swing look & feel
       try {  
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
       } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
        System.out.print(ex.getMessage());
       }
       
       
       ShopForm f1 = new ShopForm();
       SwingUtilities.invokeLater(f1);
       
       
       //Client c = new Client();
       
       /*
        for (Field f : Client.class.getFields() ){
            System.out.println(f.getName());
        }
       */
    }
    
}
