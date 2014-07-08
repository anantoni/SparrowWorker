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
import javax.net.ssl.SSLServerSocketFactory;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.HttpService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author thomas
 */

    class RequestListenerThread extends Thread {

        private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
        private final ServerSocket serversocket;
        private final HttpService httpService;
        private final ExecutorService connectionHandlerExecutor;
        
        public RequestListenerThread(
                final int port,
                final HttpService httpService,
                final SSLServerSocketFactory sf) throws IOException {
            this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
            this.serversocket = sf != null ? sf.createServerSocket(port) : new ServerSocket(port);
            this.httpService = httpService;
            // only 4 connections can run concurrently
            connectionHandlerExecutor = Executors.newFixedThreadPool(4);
            System.out.println("Request Listener Thread created");
        }

        @Override
        public void run() {
            System.out.println("Listening on port " + this.serversocket.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    System.out.println("Incoming connection from " + socket.getInetAddress());
                    HttpServerConnection conn = this.connFactory.createConnection(socket);

                    //Thread t = new TaskExecutorThread(this.httpService, conn);
                    // Initialize the pool
                    Thread connectionHandler = new ConnectionHandlerThread(this.httpService, conn);             
                    
                    connectionHandler.setDaemon(true);
                    //System.out.println("print #2");
                    connectionHandlerExecutor.execute(connectionHandler);
                    System.out.println("\tConnection Handler Thread created");
                    //t.start();
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    System.err.println("I/O error initialising connection thread: "
                            + e.getMessage());
                    break;
                }
            }
            // when the listener is interupted shutdown the pool
            // and wait for any Connection Handler threads still running
            connectionHandlerExecutor.shutdown();
            while (!connectionHandlerExecutor.isTerminated()) {
            }
            System.out.println("Finished all connection handler threads");
            
        }
    }
