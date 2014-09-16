/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpworker;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.HttpComm;

/**
 *
 * @author anantoni
 */

// This is used for late binding
public class ProbeResponseThread extends Thread{
        private String schedulerURL = null;
        private String jobID = null;
        private ThreadPoolExecutor taskExecutor = null;
    
        public ProbeResponseThread(String schedulerURL, String jobID, ThreadPoolExecutor taskExecutor ) {
            this.schedulerURL = schedulerURL;
            this.jobID = jobID;
            this.taskExecutor = taskExecutor;
        }

        
        @Override
        public void run() {
            try {
                String taskToProcess = HttpComm.lateBindingProbeResponse(schedulerURL, jobID);
                
                // parse task to process
                if (taskToProcess.equals("NOOP")) {
                        //do nothing
                }
                else {
                    taskExecutor.submit(new TaskExecutorThread(taskToProcess));
            }
                } catch (Exception ex) {
                Logger.getLogger(ProbeResponseThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
}

