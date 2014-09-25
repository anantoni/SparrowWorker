/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package httpworker;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLServerSocketFactory;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

/**
 *
 * @author thomas
 */
public class HttpWorker {
     
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        int fixedExecutorSize = 4;
        
        if (args.length != 2) {
            System.err.println("Invalid command line parameters for worker");
            System.exit(-1);
        }
        
        // Set worker mode
        String mode = args[1];
        // Creating fixed size executor
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(fixedExecutorSize, fixedExecutorSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

        int port = Integer.parseInt(args[0]);

        // Set up the HTTP protocol processor
        HttpProcessor httpproc = HttpProcessorBuilder.create()
                .add(new ResponseDate())
                .add(new ResponseServer("Test/1.1"))
                .add(new ResponseContent())
                .add(new ResponseConnControl()).build();

        // Set up request handler (either generic or late binding)
        UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
        if (mode.equals("--late")) 
            reqistry.register("*", new LateBindingRequestHandler(taskExecutor));
        else
            reqistry.register("*", new GenericRequestHandler(taskExecutor));

        
        // Set up the HTTP service
        HttpService httpService = new HttpService(httpproc, reqistry);

        SSLServerSocketFactory sf = null;
        // SSL code removed as it is not needed

        // create a thread to listen for possible scheduler available connections
        Thread t = new RequestListenerThread(port, httpService, sf);
        System.out.println("Request Listener Thread created");
        t.setDaemon(false);
        t.start();
        
        // main thread should wait for the listener to exit before shutdown the
        // task executor pool
        t.join();
        
        // shutdown task executor pool and wait for any taskExecutor thread
        // still running
        taskExecutor.shutdown();
        while (!taskExecutor.isTerminated()) {}
        
        System.out.println("Finished all task executor threads");
    }    
}