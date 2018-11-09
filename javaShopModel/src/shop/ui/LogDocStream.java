/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;


import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.SwingUtilities;

/**
 *
 * @author k
 */
public class LogDocStream extends OutputStream{
    
    private Document logDoc;
    private DateTimeFormatter timeFormat;
    
    
    /**
    * create new LogDocStream
    * if you use this for default output always use System.out.print
    * @param logDoc swing Document to write to
    */
    public LogDocStream(Document logDoc){
        timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        this.logDoc = logDoc;
    }
    
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException{
        //current length of the document
        int docOffset = logDoc.getLength();
        //make timestamp
        String timestamp = LocalTime.now().format(timeFormat);
        String msgS = new String(b, off, len);
        
        String message = String.format("<%s> %s\n", timestamp, msgS); //%s - Thread.currentThread().getName(),
        
        //write from swing's event thread 
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    logDoc.insertString(docOffset, message, null);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    //need to overrride, but shouldn't be used
    @Override
    public void write(int b) throws IOException {
        int docOffset = logDoc.getLength();
        SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run(){
                    try {
                        logDoc.insertString(docOffset, String.valueOf((char)b), null);
                    } catch (BadLocationException ex) {
                        ex.printStackTrace(); 
                    }
                }
            });
    }
    

    
}
