/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package httpworker;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author thomas
 */
class GenericRequestHandler implements HttpRequestHandler  {
        private final ThreadPoolExecutor taskExecutor;         

        // Pass reference to the requestsQueue to the GenericRequestHandler
         public GenericRequestHandler(ThreadPoolExecutor taskExecutor) {
                super();
                this.taskExecutor = taskExecutor;
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
                HttpEntity httpEntity = ((HttpEntityEnclosingRequest) request).getEntity();
                String entity = EntityUtils.toString(httpEntity);
                System.out.println("Incoming entity content (string): " + entity);

                // Parse HTTP request
                Map<String,String> requestArgs = parseHttpSchedulerRequest(entity);
                StringEntity stringEntity;
                // If task request
                if (requestArgs.size() == 3) {
                        assert requestArgs.containsKey("job-id") && requestArgs.containsKey("task-id") && requestArgs.containsKey("task-command");
                        int jobID = Integer.parseInt(requestArgs.get("job-id"));
                        int taskID = Integer.parseInt(requestArgs.get("task-id"));
                        String taskToProcess = requestArgs.get("task-command");
                        
                        // replace all + with spaces
                        taskToProcess = taskToProcess.replaceAll("\\+", " ");
                        Thread taskExecutorThread = new TaskExecutorThread(jobID, taskID, taskToProcess);
                        Future threadMonitor = taskExecutor.submit(taskExecutorThread);

                        try {
                                // the main thread should wait until the submitted thread
                                // finishes its computation
                                threadMonitor.get();
                        } catch (InterruptedException | ExecutionException ex) {
                                Logger.getLogger(GenericRequestHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        response.setStatusCode(HttpStatus.SC_OK);
                        stringEntity = new StringEntity("result:success");
                } 
                // if probe or heartbeat respond immediatelly
                else if (requestArgs.size() == 1) {
                        assert requestArgs.containsKey("probe") || requestArgs.containsKey("heartbeat");
                        response.setStatusCode(HttpStatus.SC_OK);

                        // probe response
                        if (requestArgs.containsKey("probe")){
                            int workedTasks = taskExecutor.getPoolSize() + taskExecutor.getActiveCount();
                            response.setStatusCode(HttpStatus.SC_OK);
                            stringEntity = new StringEntity(Integer.toString(workedTasks));
                        }
                        // heartbeat response
                        else{
                            response.setStatusCode(HttpStatus.SC_OK);
                            stringEntity = new StringEntity("result:success");
                        }
                }
                // else fail
                else{
                        response.setStatusCode(HttpStatus.SC_OK);
                        stringEntity = new StringEntity("result:fail");
                }
                response.setEntity(stringEntity); 
        }
    }

    // Parse HTTP requests with the following form: 
    // job-id=1&task-command=sleep+240s
    Map<String, String> parseHttpSchedulerRequest(String httpRequest) {
        String[] requestArguments = httpRequest.split("&");
        Map<String, String> argsMap = new LinkedHashMap<>();

        if (requestArguments.length != 3 && requestArguments.length != 1 ) {
            System.err.println("Invalid HTTP request: " + httpRequest);
            return null; 
        }
        else if (requestArguments.length == 3) {
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
                    if ( keyValuePair[0].equals("task-id")) {
                        argsMap.put( keyValuePair[0], keyValuePair[1]);
                    }
                    else {
                        System.err.println("Invalid argument - expecting task-id");
                    }
                }
                else if (counter == 2) {
                    if ( keyValuePair[0].equals("task-command") ) {
                        argsMap.put( keyValuePair[0], keyValuePair[1]);
                    }
                    else {
                        System.err.println("Invalid argument - expecting task-commands");
                    }
                }
                counter++;
            }
        }
        else if (requestArguments.length == 1) {
                String[] keyValuePair = requestArguments[0].split("=");
                switch (keyValuePair[0]) {
                    case "probe":
                        argsMap.put( keyValuePair[0], keyValuePair[1]);
                        break;
                    case "heartbeat":
                        argsMap.put( keyValuePair[0], keyValuePair[1]);
                        break;
                    default:
                        System.err.println("Invalid argument - expecting probe or heartbeat request");
                        break;
            }
        }
        return argsMap;
    }
}

