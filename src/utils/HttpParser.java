/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anantoni
 */
public class HttpParser {

    // Parse HTTP requests with the following form: 
    // job-id=1&task-command=sleep+240s
    public static Map<String, String> parseLateBindingSchedulerRequest(String httpRequest) {
        //System.out.println(httpRequest);
        Map<String, String> argsMap = new LinkedHashMap<>();
        
        String result = "";
        try {
            result = java.net.URLDecoder.decode(httpRequest, "UTF-8");
            //System.out.println("Decoded request: " + result);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HttpParser.class.getName()).log(Level.SEVERE, null, ex);
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
    
    public static Map<String, String> parseHttpSchedulerRequest(String httpRequest) {
        String[] requestArguments = httpRequest.split("&");
        Map<String, String> argsMap = new LinkedHashMap<>();

        if (requestArguments.length != 1 && requestArguments.length != 2 ) {
            System.err.println("Invalid HTTP request: " + httpRequest);
            return null; 
        }
        else if (requestArguments.length == 2) {
            int counter = 0;
            for ( String arg : requestArguments ) {
                String[] keyValuePair = arg.split("=");
                if (counter == 0) {
                    if ( keyValuePair[0].equals("task-duration") ) {
                        argsMap.put( keyValuePair[0], keyValuePair[1]);
                    }
                    else {
                        System.err.println("Invalid argument - expecting task-duration");
                    }
                }
                else if (counter == 1) {
                    if ( keyValuePair[0].equals("task-quantity") ) {
                        argsMap.put( keyValuePair[0], keyValuePair[1]);
                    }
                    else {
                        System.err.println("Invalid argument - expecting task-quantity");
                    }
                }
                counter++;
            }
        }
        else if (requestArguments.length == 1) {
                String[] keyValuePair = requestArguments[0].split("=");
                switch (keyValuePair[0]) {
                    case "probe":
                        argsMap.put(keyValuePair[0], keyValuePair[1]);
                        break;
                    case "heartbeat":
                        argsMap.put(keyValuePair[0], keyValuePair[1]);
                        break;
                    case "task-duration":
                        argsMap.put(keyValuePair[0], keyValuePair[1]);
                        break;
                    default:
                        System.err.println("Invalid argument - expecting probe or heartbeat request");
                        break;
            }
        }
        return argsMap;
    }
}
