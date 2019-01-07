/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;

import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import shop.models.Employee;
import shop.models.Receipt;
import shop.models.BoughtProduct;
import java.util.ArrayList;
import java.util.HashMap;
import shop.models.Shop;

/**
 *
 * @author k
 */
public class Dialogues {
    
    public static final String OVERWRITE_TXT = "This will overwrite the currently selected item.";
    public static final String DELETE_TXT = "This will delete the currently selected item\n"
            + "(it will still show up in reports, but will be able to be restored only from the database itself).";
    
    /**
     * Open a yes/no dialogue
     * @param text question text
     * @return true if yes picked
     */
    public static boolean confirmYesNo(String text){
        int response = JOptionPane.showConfirmDialog(null, text, "Confirm",
                                        JOptionPane.YES_NO_OPTION, 
                                        JOptionPane.QUESTION_MESSAGE);
        
        if (response == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }
    
    /**
     * ask for new password
     * @return password as plain string
     */
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
    
    /**
     * displays info about an employee as a dialogue
     * @param e to show info about
     */
    public static void showEmpInfo(Employee e){
        String infoTemplate = "%10s%10s\nusername:  %s\nposition:     %s";
        String position = (e.accessLvl == 0)? "cashier" : "cashier/manager";
        JOptionPane.showMessageDialog(null, 
                String.format(infoTemplate, e.firstname, e.lastname, e.username, position),
                String.format("logged in as employee %d", e.employeeId), 
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void showReceipt(String[] headers, ArrayList<BoughtProduct> bps, HashMap<Integer, String> prodNames, double total){
        int targetWidth = 50;
        
        StringBuilder text = new StringBuilder();
        int tmpOffset;
        String tmpName;
        String tmpPrice;
        int tmpCount;
        String tmpMultipleF = "%d x %.2f";
        String tmpMultiple;
        
        for(int i = 0; i < headers.length; i++){
            tmpOffset = targetWidth/2 + headers[i].length()/2;
            text.append(String.format("%"+tmpOffset+"s\n", headers[i]));
            if(i == 0 || i == 3)
                text.append("\n");
        }
        
        text.append("\n");
        
        for(BoughtProduct bp: bps){
            tmpName = prodNames.get(bp.productId);
            tmpCount = bp.productCount;
            
            text.append(tmpName).append("\n");
            tmpMultiple = String.format(tmpMultipleF, tmpCount, bp.currentPrice);
            tmpPrice = String.format("%.2f", bp.currentPrice*tmpCount);
            text.append(String.format("%20s"+" %"+(targetWidth-21)+"s\n", tmpMultiple, tmpPrice));      
        }
        
        text.append("\n");
        String totalStr = String.format("%.2f", total);

        text.append(String.format("%"+targetWidth+"s", "Total: " + totalStr));
        
        System.out.println(text);
        
    }
}
