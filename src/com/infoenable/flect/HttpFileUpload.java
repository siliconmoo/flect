package com.infoenable.flect;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

public class HttpFileUpload {
	int  serverResponseCode;
    public byte[] ba;
	public int uploadFile(String urlServer, String targetFileName) {
        
		Log.d("",String.format("bytes %d", ba.length));
		ByteArrayInputStream bis = 
       	  new ByteArrayInputStream(ba);

        
        String fileName = targetFileName;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;  
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "****abdefghijklmnop****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer = ba.clone();
        int maxBufferSize = 1 * 1024 * 1024; 
        File sourceFile = null;
          
        
        {
             try { 
                  String uploadServerUri = "http://www.infoenable.com/ieweb/helthi/protocols/uploadFlect.php";
                   // open a URL connection to the Servlet
                 URL url = new URL(uploadServerUri);
                  
                 // Open a HTTP  connection to  the URL
                 conn = (HttpURLConnection) url.openConnection(); 
                 conn.setDoInput(true); // Allow Inputs
                 conn.setDoOutput(true); // Allow Outputs
                 //conn.setChunkedStreamingMode(0);
                 conn.setUseCaches(false); // Don't use a Cached Copy
                 conn.setRequestMethod("POST");
                 conn.setRequestProperty("Connection", "Keep-Alive");
                 conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                 conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                 conn.setRequestProperty("uploaded_file", fileName); 
                  
                 dos = new DataOutputStream(conn.getOutputStream());
        
                 dos.write((twoHyphens + boundary + lineEnd).getBytes()); 
                 dos.write(("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                           + fileName + "\"" + lineEnd).getBytes());
                  
                 dos.write(lineEnd.getBytes());
                 
                 for (byte b : buffer){
                 dos.write(b);
                 }
                 
                 // send multipart form data necesssary after file data...
                 dos.write(lineEnd.getBytes());
                 dos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
        
            
                 // Responses from the server (code and message)
                 serverResponseCode = conn.getResponseCode();
                 String serverResponseMessage = conn.getResponseMessage();
                   
                 Log.i("uploadFile", "HTTP Response is : "
                         + serverResponseMessage + ": " + serverResponseCode);
                  
                 if(serverResponseCode == 200){
                      
                     Log.d("FlectHTTP","200");               
                 }    
                  
                 
                 conn.disconnect();
                 dos.flush();                
                 dos.close();
                   
            } catch (MalformedURLException ex) {
                 
                ex.printStackTrace();
                                  
                Log.e("FlectHTTP", "error: " + ex.getMessage(), ex);  
            } catch (Exception e) {
                 
                Log.e("Upload file to server Exception", "Exception : "
                                                 + e.getMessage(), e);  
            }
            return serverResponseCode; 
             
         } // End else block 
       } 
}
