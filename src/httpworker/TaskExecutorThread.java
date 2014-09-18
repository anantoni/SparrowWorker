/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package httpworker;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import utils.StatsLog;

/**
 *
 * @author thomas
 */
    class TaskExecutorThread extends Thread {

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
            System.out.println("\t\t Task executor thread is running the following command: " + taskCommand);
            HttpContext context = new BasicHttpContext(null);
            try {
                    Process process = Runtime.getRuntime().exec(taskCommand);
                    process.waitFor();
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(TaskExecutorThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            StatsLog.writeToLog("Job #" + jobID + " task # " + taskID + " command: " + taskCommand + " finished");
        }
    }