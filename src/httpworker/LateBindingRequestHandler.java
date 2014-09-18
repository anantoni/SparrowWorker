/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package httpworker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
 * @author anantoni
 */
class LateBindingRequestHandler implements HttpRequestHandler  {
        private final ThreadPoolExecutor taskExecutor;         

        // Pass reference to the requestsQueue to the RequestHandler
         public LateBindingRequestHandler(ThreadPoolExecutor taskExecutor) {
                super();
                this.taskExecutor = taskExecutor;
        }

        @Override
        public void handle( final HttpRequest request,
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
                    System.out.println("Received probe");
                    response.setStatusCode(HttpStatus.SC_OK);

                    // probe response
                    Thread probeResponseThread = new ProbeResponseThread(requestArgs.get("scheduler-url"), requestArgs.get("job-id"), taskExecutor);
                    Future threadMonitor = taskExecutor.submit(probeResponseThread);

//                        try {
//                                // the main thread should wait until the submitted thread
//                                // finishes its computation
//                                threadMonitor.get();
//                        } catch (InterruptedException | ExecutionException ex) {
//                                Logger.getLogger(LateBindingRequestHandler.class.getName()).log(Level.SEVERE, null, ex);
//                        }
                    response.setStatusCode(HttpStatus.SC_OK);
                    stringEntity = new StringEntity("result:success");
                } 
                // if probe or heartbeat respond immediatelly
                else if (requestArgs.size() == 1) {
                    assert requestArgs.containsKey("heartbeat");
                    response.setStatusCode(HttpStatus.SC_OK);

                    response.setStatusCode(HttpStatus.SC_OK);
                    stringEntity = new StringEntity("result:success");
                }
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
        System.out.println(httpRequest);
        Map<String, String> argsMap = new LinkedHashMap<>();
        
        String result = "";
        try {
            result = java.net.URLDecoder.decode(httpRequest, "UTF-8");
            System.out.println("Decoded request: " + result);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LateBindingRequestHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        String[] requestArguments = result.split("&");
        
        if (requestArguments.length != 3 && requestArguments.length != 1 ) {
            System.err.println("Invalid HTTP request: " + httpRequest);
            return null; 
        }
        else if (requestArguments.length == 3) {
            int counter = 0;
            for ( String arg : requestArguments ) {
                String[] keyValuePair = arg.split("=");
                if (counter == 0) {
                    assert keyValuePair[0].equals("probe");
                    argsMap.put("probe", "yes");
                }
                else if (counter == 1) {
                    assert keyValuePair[0].equals("scheduler-url");
                    argsMap.put("scheduler-url", keyValuePair[1]);
                }
                else if (counter == 2) {
                    assert keyValuePair[0].equals("job-id");
                    argsMap.put("job-id", keyValuePair[1]);
                }
                counter++;
            }
        }
        else if (requestArguments.length == 1) {
            String[] keyValuePair = requestArguments[0].split("=");
            assert keyValuePair[0].equals("heartbeat");
            argsMap.put( keyValuePair[0], keyValuePair[1]);
        }
        return argsMap;
    }
}

