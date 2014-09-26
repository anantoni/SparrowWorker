/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package httpworker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.StatsLog;

/**
 *
 * @author thomas
 */
    class TaskExecutorThread implements Runnable {

        //private final String taskCommand;
        private final int taskDuration;

        TaskExecutorThread(int taskDuration) {
            super();
            this.taskDuration = taskDuration;
            //System.out.println("\t\tTask executor thread created");
        }
        
//        TaskExecutorThread(int jobID, int taskID, String taskCommand) {
//            super();
//            this.taskDuration = taskDuration;
//        }

        @Override
        public void run() {
            //System.out.println("\t\t Task executor thread is running the following command: " + taskCommand);
            try {
                    //Process process = Runtime.getRuntime().exec(taskCommand);
                    //process.waitFor();
//                    ProcessBuilder pb = new ProcessBuilder(taskCommand);
//                    Process p  = new ProcessBuilder("/bin/bash", System.getProperty("user.dir") +"/" + taskCommand).start();
//                    p.waitFor();
                Thread.sleep(taskDuration);
            } catch (InterruptedException ex) {
                Logger.getLogger(TaskExecutorThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            Date dNow = new Date( );
            SimpleDateFormat ft = new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
            StatsLog.writeToLog( ft.format(dNow) + " sleep " + taskDuration + " finished");
        }
    }