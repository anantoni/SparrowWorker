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
        // responding to a probe in late binding suggests that the worker is idle and availabe 
        // to receive tasks for execution
        System.out.println("Responding to probe - scheduler url: " + schedulerURL + " job id: " + jobID);
        try {
            String taskToProcess = HttpComm.lateBindingProbeResponse(schedulerURL, jobID);
            System.out.println( "Task to process: " + taskToProcess);
            // parse task to process
            if (taskToProcess.equals("NOOP")) {
                    //do nothing
            }
            else {
                String[] pieces = taskToProcess.split("&");
                // create new task executor thread to execute command and submit to task
                // executor
                taskExecutor.submit(new TaskExecutorThread( Integer.parseInt(pieces[0]), 
                                                                                            Integer.parseInt(pieces[1]),
                                                                                            pieces[2]));
            }
        } catch (Exception ex) {
            Logger.getLogger(ProbeResponseThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

