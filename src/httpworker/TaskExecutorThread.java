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

/**
 *
 * @author thomas
 */
    class TaskExecutorThread extends Thread {

        private final String taskToProcess;

        TaskExecutorThread(String taskToProcess) {
            super();
            this.taskToProcess = taskToProcess;
            System.out.println("\t\tTask executor thread created");
        }

        @Override
        public void run() {
            System.out.println("\t\t Task executor thread is running the following command: " + taskToProcess);
            HttpContext context = new BasicHttpContext(null);
            try {
                    Process process = Runtime.getRuntime().exec(taskToProcess);
                    process.waitFor();
            } catch (IOException ex) {
                System.err.println("I/O error: " + ex.getMessage());

            } catch (InterruptedException ex) {
                Logger.getLogger(TaskExecutorThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }