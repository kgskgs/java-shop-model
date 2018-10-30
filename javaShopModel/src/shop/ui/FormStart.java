/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.*;
import shop.db.DatabaseDispatch;

/**
 *
 * @author K1
 */
public class FormStart extends javax.swing.JFrame implements Runnable {

    /**
     * Creates new form formStart
     */
    
    private final BlockingQueue<String> queryStrQueue;
    
    //test vars
    private int testKey = 0;
    DatabaseDispatch dbd;
    
    
    public FormStart(BlockingQueue<String> q, DatabaseDispatch dbd) {
        initComponents();
        logDoc = txtLog.getDocument();
        timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        queryStrQueue = q;
        this.dbd = dbd;
        log("started");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();
        testAddStr = new javax.swing.JButton();
        testCommit = new javax.swing.JButton();
        testLblCount = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        txtLog.setEditable(false);
        txtLog.setColumns(20);
        txtLog.setFont(new java.awt.Font("Consolas", 0, 12)); // NOI18N
        txtLog.setRows(5);
        txtLog.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        jScrollPane1.setViewportView(txtLog);

        testAddStr.setText("add row");
        testAddStr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testAddStrActionPerformed(evt);
            }
        });

        testCommit.setText("force commit");
        testCommit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testCommitActionPerformed(evt);
            }
        });

        testLblCount.setText("0");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(137, 137, 137)
                                .addComponent(testAddStr)
                                .addGap(121, 121, 121)
                                .addComponent(testCommit))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(258, 258, 258)
                                .addComponent(testLblCount, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(115, 115, 115)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(testAddStr)
                    .addComponent(testCommit))
                .addGap(35, 35, 35)
                .addComponent(testLblCount)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 116, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void testAddStrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testAddStrActionPerformed
        try {
            testLblCount.setText(Integer.toString((testKey%10)+1));
            queryStrQueue.put(String.format("insert into testtbl values (%d,'val %d')", testKey, testKey));
            log(String.format("insert into testtbl values (%d,'val %d')", testKey, testKey));
            testKey += 1;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            
        }
        
    }//GEN-LAST:event_testAddStrActionPerformed

    private void testCommitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testCommitActionPerformed
        dbd.forceCommit();
    }//GEN-LAST:event_testCommitActionPerformed

    /**
     * @param args the command line arguments
     */


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton testAddStr;
    private javax.swing.JButton testCommit;
    private javax.swing.JLabel testLblCount;
    private javax.swing.JTextArea txtLog;
    // End of variables declaration//GEN-END:variables

    //custom vars declaration
    private Document logDoc;
    private DateTimeFormatter timeFormat;
    
    @Override
    public void run() {
        setVisible(true);
    }
    
     /**
     * log a string in the gui textarea
     */
    public void log(String text){
        int offset = logDoc.getLength();

        String timestamp = LocalTime.now().format(timeFormat);

        String message = String.format("<%s - %s> %s\n",Thread.currentThread().getName(), timestamp, text);

        try {
            logDoc.insertString(offset, message, null);
        } catch (BadLocationException e) {
            e.printStackTrace(); 
        }
    }
}
