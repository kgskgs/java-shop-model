/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
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
 * This stream will print to a JTextArea
 * adds a timestamp
 * used for the log in ShopForm
 * @author k
 */
public class LogDocStream extends OutputStream{
    
    private Document logDoc;
    private DateTimeFormatter timeFormat;
    private final String lineBreak = System.lineSeparator();
    
    
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
        if (!lineBreak.equals(msgS)){
            msgS = msgS.replaceAll(lineBreak, "");

            String message = String.format("<%s> %s\n", timestamp, msgS); //%s - Thread.currentThread().getName(),

            //write from swing's event thread 
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        logDoc.insertString(docOffset, message, null);
                    } catch (BadLocationException ex) {
                        System.out.println(ex.getMessage()); //ex.printStackTrace();
                    }
                }
            });
        }
    }

    //need to overrride, but shouldn't be used
    @Override
    public void write(int b) throws IOException {
        int docOffset = logDoc.getLength();
        SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run(){
                    try {
                        if (lineBreak.charAt(0) != ((char)b))
                            logDoc.insertString(docOffset, String.valueOf((char)b), null);
                    } catch (BadLocationException ex) {
                        System.out.println(ex.getMessage()); //ex.printStackTrace(); 
                    }
                }
            });
    }
    

    
}
