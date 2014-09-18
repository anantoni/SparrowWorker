/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anantoni
 */

// ask what happens if its final;
public class StatsLog {
    private static PrintWriter logWriter;
    static{
        try{
            logWriter = new PrintWriter("./log/stats.txt", "UTF-8");
        }   catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(StatsLog.class.getName()).log(Level.SEVERE, null, ex);
        }  
    }
    
//    public StatsLog() {
//        try{
//            logWriter = new PrintWriter("the-file-name.txt", "UTF-8");
//        }   catch (FileNotFoundException | UnsupportedEncodingException ex) {
//            Logger.getLogger(StatsLog.class.getName()).log(Level.SEVERE, null, ex);
//        }  
//    }
    
    public static synchronized void writeToLog(String line) {
        logWriter.println(line);
        logWriter.flush();
    }

}
