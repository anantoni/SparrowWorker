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

        private final String taskCommand;
        private final int jobID;
        private final int taskID;

        TaskExecutorThread(int taskID, String taskCommand) {
            super();
            this.taskCommand = taskCommand;
            this.jobID = -1;
            this.taskID = taskID;
            //System.out.println("\t\tTask executor thread created");
        }
        
        TaskExecutorThread(int jobID, int taskID, String taskCommand) {
            super();
            this.taskCommand = taskCommand;
            this.taskID = taskID;
            this.jobID = jobID;
        }

        @Override
        public void run() {
            //System.out.println("\t\t Task executor thread is running the following command: " + taskCommand);
            try {
                    //Process process = Runtime.getRuntime().exec(taskCommand);
                    //process.waitFor();
                    ProcessBuilder pb = new ProcessBuilder(taskCommand);
                    Process p  = new ProcessBuilder("/bin/bash", System.getProperty("user.dir") +"/" + taskCommand).start();
                    p.waitFor();
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(TaskExecutorThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            Date dNow = new Date( );
            SimpleDateFormat ft = new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
            StatsLog.writeToLog( ft.format(dNow) + " Job #" + jobID + " task # " + taskID + " command: " + taskCommand + " finished");
        }
    }