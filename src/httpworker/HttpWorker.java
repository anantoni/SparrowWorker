/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package httpworker;

import java.net.URL;
import java.security.KeyStore;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import org.apache.http.examples.ElementalHttpServer;
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
 * @author anantoni
 */
public class HttpWorker {
     
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        int fixedExecutorSize = 4;
        //Creating fixed size executor
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(fixedExecutorSize, fixedExecutorSize,0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        // TODO code application logic here
        int port = 8080;

        // Set up the HTTP protocol processor
        HttpProcessor httpproc = HttpProcessorBuilder.create()
                .add(new ResponseDate())
                .add(new ResponseServer("Test/1.1"))
                .add(new ResponseContent())
                .add(new ResponseConnControl()).build();

        // Set up request handlers
        UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
        reqistry.register("*", new RequestHandler(taskExecutor));

        // Set up the HTTP service
        HttpService httpService = new HttpService(httpproc, reqistry);

        SSLServerSocketFactory sf = null;
        if (port == 8443) {
            // Initialize SSL context
            ClassLoader cl = ElementalHttpServer.class.getClassLoader();
            URL url = cl.getResource("my.keystore");
            if (url == null) {
                System.out.println("Keystore not found");
                System.exit(1);
            }
            KeyStore keystore  = KeyStore.getInstance("jks");
            keystore.load(url.openStream(), "secret".toCharArray());
            KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            kmfactory.init(keystore, "secret".toCharArray());
            KeyManager[] keymanagers = kmfactory.getKeyManagers();
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(keymanagers, null, null);
            sf = sslcontext.getServerSocketFactory();
        }

        Thread t = new RequestListenerThread(port, httpService, sf);
        System.out.println("Thread created");
        t.setDaemon(false);
        t.start();
        t.join();
        taskExecutor.shutdown();
        while (!taskExecutor.isTerminated()) {
        }
        System.out.println("Finished all task executor threads");

    }
    
}