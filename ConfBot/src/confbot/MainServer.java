/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package confbot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Vector;

/**
 *
 * @author Aman
 */
public class MainServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        String lockfilename = ".." + File.separator + "runtime" + File.separator + " queue.lock";
        File fqlock = new File(lockfilename);
        System.out.println(fqlock + (fqlock.exists() ? " is found " : " is missing "));
        
        
        while (true) {
            // Sleep for some time before checking queue again            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            
            // Wait if lock file exists
            int x = 0;
            while (fqlock.exists()) {
                System.out.println(x); x++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            
            // Create Lock file
            System.out.println("creating lock file...");
            try {
                fqlock.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            
            // Read queue file
            System.out.println("reading queue file....");
            String queuefilename = ".." + File.separator + "runtime" + File.separator + "queue.txt";
            File fqueue = new File(queuefilename);
            
            if (!fqueue.exists()) {
                // Delete lock file
                fqlock.delete();
                continue;
            }
            
            Vector v = fileToVector(fqueue);
            Enumeration en = v.elements();
            while(en.hasMoreElements()) {
                String qComm = (String)en.nextElement(); // queuedCommand
                System.out.println(qComm);
                if (qComm.startsWith("START")) {
                    String[] cmdArgs = extractArgs(qComm.substring(qComm.indexOf('"')), 5 );
                    new Main(cmdArgs);
                }
            }
            
            // TODO Delete queue file
            fqueue.delete();
            
            // Delete lock file
            fqlock.delete();
            
        }// WHILE LOOP ENDS HERE
    
    }
    
    public static Vector fileToVector(File inFile) {
        Vector v = new Vector();
        String inputLine;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(inFile)));

            while ((inputLine = br.readLine()) != null) {
                v.addElement(inputLine.trim());
            }
            br.close();
        } // Try
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return (v);
    }

    private static String[] extractArgs(String str, int args) {
        String[] ret = new String[args];
        int x = 0;
        while (str.indexOf('"') != -1 && x<args ) {
            int index1 = str.indexOf('"');
            int index2 = str.indexOf('"', index1+1);
            ret[x] = str.substring(index1+1, index2);
            
            System.out.println(ret[x++]);
            
            str = str.substring(index2+1);
        }
        
        return ret;
    }

}
