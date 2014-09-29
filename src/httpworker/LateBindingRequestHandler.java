/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package httpworker;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
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
import static utils.HttpParser.parseLateBindingSchedulerRequest;

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
        public void handle( final HttpRequest request, final HttpResponse response,
                                             final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }

            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity httpEntity = ((HttpEntityEnclosingRequest) request).getEntity();
                String entity = EntityUtils.toString(httpEntity);
                //System.out.println("Incoming entity content (string): " + entity);

                // Parse HTTP request
                Map<String,String> requestArgs =parseLateBindingSchedulerRequest(entity);
                StringEntity stringEntity;
                // if probe request
                if (requestArgs.size() == 3) {
                    
                    //System.out.println("Received probe from: " + request.getRequestLine().getUri());
                    response.setStatusCode(HttpStatus.SC_OK);

                    // initialize probe response thread
                    Thread probeResponseThread = new LateBindingProbeResponseThread(requestArgs.get("scheduler-url"), 
                                                                                                                                 requestArgs.get("job-id"), 
                                                                                                                                 taskExecutor);
                    taskExecutor.execute(probeResponseThread);

                    stringEntity = new StringEntity("result:success");
                } 
                // if heartbeat respond immediatelly
                else if (requestArgs.size() == 1) {
                    assert requestArgs.containsKey("heartbeat");
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
}

