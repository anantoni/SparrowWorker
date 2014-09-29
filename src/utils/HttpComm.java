/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author anantoni
 */
public class HttpComm {
    private static final String schedulerURL = "http://localhost:51000";
    
    static final CloseableHttpClient httpClient;
    static {
        // Create an HttpClient with the ThreadSafeClientConnManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        httpClient = HttpClients.custom().setConnectionManager(cm).build();
    }
    
    public static String lateBindingProbeResponse(String schedulerURL, String jobID) throws Exception {        
        Map<String, String> postArguments = new LinkedHashMap();        
        postArguments.put( "job-id", jobID );
        String result = Post(schedulerURL, postArguments);
        
        return result;
    }
    
     public static String Post( String workerURL, Map<String, String> postArguments) throws Exception {
        HttpPost httpPost = new HttpPost(workerURL);
        httpPost.setProtocolVersion(HttpVersion.HTTP_1_1);
        List <NameValuePair> nvps = new ArrayList <>();
        for (String key :postArguments.keySet())
            nvps.add( new BasicNameValuePair( key, postArguments.get(key) ) );

        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            //System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            String s = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
            return s;
        }
        finally {
            httpPost.releaseConnection();
        }
    }
    
//    public static String Post( String schedulerURL, Map<String, String> postArguments) throws Exception {
//        HttpPost httpPost = new HttpPost(schedulerURL);
//        httpPost.setProtocolVersion(HttpVersion.HTTP_1_1);
//        List <NameValuePair> nvps = new ArrayList <>();
//        for (String key :postArguments.keySet())
//            nvps.add( new BasicNameValuePair( key, postArguments.get(key) ) );
//
//        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
//        String s = "";
//        
//        HttpContext context = HttpClientContext.create();
//        CloseableHttpResponse response = httpClient.execute(httpPost, context);
//        try {
//            HttpEntity entity = response.getEntity();
//            s = EntityUtils.toString(entity);
//            EntityUtils.consume(entity);
//            response.close();
//        }   
//        catch (IOException ex) {
//            Logger.getLogger(HttpComm.class.getName()).log(Level.SEVERE, null, ex);
//        } 
//        finally {
//            httpPost.releaseConnection();
//        }
//        return s;
//    }
//    
//    public static String AsyncPost( String schedulerURL, Map<String, String> postArguments) throws Exception {
//        HttpPost httpPost = new HttpPost(schedulerURL);
//        httpPost.setProtocolVersion(HttpVersion.HTTP_1_1);
//        List <NameValuePair> nvps = new ArrayList <>();
//        for (String key :postArguments.keySet())
//            nvps.add( new BasicNameValuePair( key, postArguments.get(key) ) );
//
//        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
//        String s = "";
//        
//        HttpContext context = HttpClientContext.create();
//        CloseableHttpResponse response = httpClient.execute(httpPost, context);
//        
//        String inputLine = "" ;
//        BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//        try {
//              while ((inputLine = in.readLine()) != null) {
//                     System.out.println(inputLine);
//              }
//              in.close();
//         } catch (IOException e) {
//              e.printStackTrace();
//         } finally {
//            response.close();
//            httpPost.releaseConnection();
//        }
//         s = inputLine;
////        try {
////            HttpEntity entity = response.getEntity();
////            s = EntityUtils.toString(entity);
////            EntityUtils.consume(entity);
////        }   
////        catch (IOException ex) {
////            Logger.getLogger(HttpComm.class.getName()).log(Level.SEVERE, null, ex);
////        } 
////        finally {
////            response.close();
////            httpPost.releaseConnection();
////        }
//        return s;
//    }
    
}
