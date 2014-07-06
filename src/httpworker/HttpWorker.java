/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package httpworker;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.StringEntity;
import org.apache.http.examples.ElementalHttpServer;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.util.EntityUtils;

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
        //Creating shared object to store requested tasks
        BlockingQueue taskQueue = new LinkedBlockingQueue();
        
        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        
        //threadPool.execute(new Consumer("1", requestQueue));
        //threadPool.execute(new Consumer("2", requestQueue);
        //threadPool.execute(new Consumer("3", requestQueue));
        //threadPool.execute(new Consumer("4", requestQueue);
        
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
        reqistry.register("*", new RequestHandler(taskQueue));

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
    }

    static class RequestHandler implements HttpRequestHandler  {
        private final BlockingQueue taskQueue;         
        
        // Pass reference to the requestsQueue to the RequestHandler
        public RequestHandler(BlockingQueue taskQueue) {
            super();
            this.taskQueue = taskQueue;     
        }

        @Override
        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }

            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                //byte[] entityContent = EntityUtils.toByteArray(entity);
                String entityString = EntityUtils.toString(entity);
                System.out.println("Incoming entity content (string): " + entityString);
                //System.out.println("Incoming entity content (bytes): " + entityContent.length);
                //String a = new String(entityContent);
               // System.out.println(a);
                
                // Parse HTTP request
                Map<String,String> requestArgs = parseHttpSchedulerRequest(entityString);
                
                // If task requst
                if (requestArgs.size() == 2) {
                    assert requestArgs.containsKey("job-id") && requestArgs.containsKey("task-command");
                    System.out.println("Adding task command to queue");
                    String taskToProcess = requestArgs.get("task-command");
                    try {
                        taskQueue.put(taskToProcess);     // Add task to the request queue
                    } catch (InterruptedException ex) {
                        Logger.getLogger(HttpWorker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }      
                // If probe request
                else if (requestArgs.size() == 1) {
                    assert requestArgs.containsKey("probe");
                }
            }
                    
            // TODO: wait for result of task processing before returning the result to the scheduler
            response.setStatusCode(HttpStatus.SC_OK);
            StringEntity entity = new StringEntity("result:success");
            response.setEntity(entity);
        }
        
        // Parse HTTP requests with the following form: 
        // job-id=1&task-command=sleep+240s
        Map<String, String> parseHttpSchedulerRequest(String httpRequest) {
            String[] requestArguments = httpRequest.split("&");
            Map<String, String> argsMap = new HashMap<>();

            if (requestArguments.length != 2 && requestArguments.length != 1 ) {
                System.err.println("Invalid HTTP request: " + httpRequest);
                return null; 
            }
            else if (requestArguments.length == 2) {
                int counter = 0;
                for ( String arg : requestArguments ) {
                    String[] keyValuePair = arg.split("=");
                    if (counter == 0) {
                        if ( keyValuePair[0].equals("job-id") ) {
                            assert keyValuePair[1].matches("[0-9]+");
                            argsMap.put( keyValuePair[0], keyValuePair[1]);
                        }
                        else {
                            System.err.println("Invalid argument - expecting job-id");
                        }
                    }
                    else if (counter == 1) {
                        if ( keyValuePair[0].equals("task-command") ) {
                            argsMap.put( keyValuePair[0], keyValuePair[1]);
                        }
                        else {
                            System.err.println("Invalid argument - expecting task-command");
                        }
                    }
                    counter++;
                }
            }
            else if (requestArguments.length == 1) {
                String[] keyValuePair = requestArguments[0].split("=");
                if ( keyValuePair[0].equals("probe") ) 
                    argsMap.put( keyValuePair[0], keyValuePair[1]);
                else 
                    System.err.println("Invalid argument - expecting probe request");
            }
            return argsMap;
        }
    }

    static class RequestListenerThread extends Thread {

        private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
        private final ServerSocket serversocket;
        private final HttpService httpService;

        public RequestListenerThread(
                final int port,
                final HttpService httpService,
                final SSLServerSocketFactory sf) throws IOException {
            this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
            this.serversocket = sf != null ? sf.createServerSocket(port) : new ServerSocket(port);
            this.httpService = httpService;
        }

        @Override
        public void run() {
            System.out.println("Listening on port " + this.serversocket.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    System.out.println("print #1");
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    System.out.println("Incoming connection from " + socket.getInetAddress());
                    HttpServerConnection conn = this.connFactory.createConnection(socket);

                    // Start worker thread
                    Thread t = new WorkerThread(this.httpService, conn);
                    t.setDaemon(true);
                    System.out.println("print #2");
                  
                    t.start();
                    System.out.println("print #3");
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    System.err.println("I/O error initialising connection thread: "
                            + e.getMessage());
                    break;
                }
            }
        }
    }

    static class WorkerThread extends Thread {

        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(
                final HttpService httpservice,
                final HttpServerConnection conn) {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }

        @Override
        public void run() {
            System.out.println("New connection thread");
            HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && this.conn.isOpen()) {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex) {
                System.err.println("Client closed connection");
            } catch (IOException ex) {
                System.err.println("I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                System.err.println("Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {}
            }
        }

    }
}
    
