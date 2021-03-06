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
public class LateBindingProbeResponseThread extends Thread{
    private String schedulerURL = null;
    private String jobID = null;
    private ThreadPoolExecutor taskExecutor = null;

    public LateBindingProbeResponseThread(String schedulerURL, String jobID, ThreadPoolExecutor taskExecutor ) {
        this.schedulerURL = schedulerURL;
        this.jobID = jobID;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void run() {
        // responding to a probe in late binding suggests that the worker is idle and availabe 
        // to receive tasks for execution
       
        try {
//            System.out.println("Responding to probe - scheduler url: " + schedulerURL + " job id: " + jobID);
            String taskToProcess = HttpComm.lateBindingProbeResponse(schedulerURL, jobID);
//            System.out.println("returned from late binding probe response");
//            System.out.println( "Task to process: " + taskToProcess);
            // parse task to process
            if (!taskToProcess.equals("NOOP")) {
                // create new task executor thread to execute command and submit to task
                // executor
                taskExecutor.execute(new TaskExecutorThread(Integer.parseInt(taskToProcess)));
            }
        } catch (Exception ex) {
            Logger.getLogger(LateBindingProbeResponseThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

