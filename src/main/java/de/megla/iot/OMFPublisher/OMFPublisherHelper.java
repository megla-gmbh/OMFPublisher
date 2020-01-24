/*******************************************************************************
 * Copyright (c) 2020 MEGLA GmbH and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MEGLA GmbH
 *******************************************************************************/

package de.megla.iot.OMFPublisher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.megla.iot.OMFPublisher.models.AssetFrameworkElement;
import de.megla.iot.OMFPublisher.models.LinkedDataMessage;
import de.megla.iot.OMFPublisher.models.LinkedValues;
import de.megla.iot.OMFPublisher.models.OMFAsset;
import de.megla.iot.OMFPublisher.models.OMFChannel;
import de.megla.iot.OMFPublisher.models.OMFTypeMessage;
import de.megla.iot.OMFPublisher.models.Property;
import de.megla.iot.OMFPublisher.models.SourceTarget;

/**
 * OMFPublisherHelper.java
 * 
 * The OMFPublisherHelper class supports the publisher by writing the individual definitions of Type, Container, 
 * and Data in a suitable JSON format. In addition, it creates the header according to the "messagetype" 
 * and makes a request to the destination system.
 */

public class OMFPublisherHelper{
	/**
	 * <b>omfPublisherOptions</b> contains option-data for example the producertoken, hostname, targeturl
	 * <b>trustAllCerts</b> 
	 * <b>logger</b> logs all messages for Debugging (Info, Warning, Error)
	 */ 
	private OMFPublisherOptions omfPublisherOptions;
	private TrustManager[] trustAllCerts;
	private static final Logger logger = LoggerFactory.getLogger(OMFPublisherHelper.class);
	
	
	/**
	 * Constructor in which the options are set and the SSL certificates are validated
	 * @param myPublisherOptions Options needed by the publisher
	 */
	OMFPublisherHelper(OMFPublisherOptions myPublisherOptions){
		this.omfPublisherOptions=myPublisherOptions;
				
		if (!this.omfPublisherOptions.getSSLVerify()) {
			// Create a trust manager that does not validate certificate chains
	        this.trustAllCerts = new TrustManager[] {new X509TrustManager() {
	                @Override
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                    return null;
	                }
	                @Override
					public void checkClientTrusted(X509Certificate[] certs, String authType) {
	                }
	                @Override
					public void checkServerTrusted(X509Certificate[] certs, String authType) {
	                }
	            }
	        };	 
	        // Install the all-trusting trust manager
	        SSLContext sc;
			try {
				sc = SSLContext.getInstance("SSL");
		        sc.init(null, trustAllCerts, new java.security.SecureRandom());
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		 	       
		        HostnameVerifier allHostsValid = new HostnameVerifier() {
		            @Override
					public boolean verify(String hostname, SSLSession session) {
		                return true;
		            }
		        };
		        
		        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		        
			} catch (NoSuchAlgorithmException | KeyManagementException ex) {
				 
				logger.error(" Error disabling certifficate validation: ", ex);
			} //catch
		} //if		
	}

    // ----------------------------------------------------------------
    //
    // Public methods
    //
    // ----------------------------------------------------------------    
	
	/**
	 * Generates a JSON string for the type message, every asset, and their channels
	 * @param listOfAssets key-value-Store of every created asset
	 * @return JSON formatted string of the type message
	 */
	public String createTypeMessage(Map<String, OMFAsset> listOfAssets) {
		Gson gson = new Gson();
		StringBuilder sb = new StringBuilder();
		Map<String, Property> properties = new HashMap<>();
		/*
		 * build the TypeMessage of the default IoTDevice 
		 */								
		Property propName = new Property("string", true);
		properties.put("Name", propName);
		OMFTypeMessage typeMessage = new OMFTypeMessage("KuraIoTDevice", null, "object", "static", properties, null);
		sb.append("[").append(gson.toJson(typeMessage)).append(",");
		/*
		 * Append every Asset and their Channels to the default TypeMessage
		 */
		for (Map.Entry<String, OMFAsset> entry : listOfAssets.entrySet()){		
        	 OMFAsset asset = entry.getValue();
        	 sb.append(asset.getTypeMessageJSON());
		 	 for (Map.Entry<String, OMFChannel> channelEntry : asset.getChannels().entrySet()){
		 		 OMFChannel channel = channelEntry.getValue();
		 		 sb.append(",").append(channel.getTypeMessageJSON()); 
	         } //for
		 	 sb.append(",");
        }//for
		if(sb.length() > 1) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("]");
		return sb.toString();
	}
	
	/**
	 * Generates the containers of all channels in all assets
	 * @param listOfAssets key-value-Store of every created asset
	 * @return JSON formatted string of the container
	 */
	public String createContainerMessage(Map<String, OMFAsset> listOfAssets){
		StringBuilder sb = new StringBuilder();
		 sb.append("[");
		 /*
		  * Append every Asset and their Channels to the ContainerMessage
		  */
		for (Map.Entry<String, OMFAsset> entry : listOfAssets.entrySet()){
        	 OMFAsset asset = entry.getValue();    	
		 	 for (Map.Entry<String, OMFChannel> channelEntry : asset.getChannels().entrySet()){	
		 		 OMFChannel channel = channelEntry.getValue();
		 		 sb.append(channel.getContainerMessageJSON()).append(",");
	         } //for	 
         } //for
		if(sb.length() > 1) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("]");
		return sb.toString();
	}
	
	/**
	 * Creates the JSON data for a data container. Appends and returns all channel data
	 * @param listOfAssets key-value-Store of every created asset
	 * @return JSON formatierter String of every channel- and asset-data 
	 */
	public String createDataValuesMessage(Map<String, OMFAsset> listOfAssets) {
		StringBuilder sb = new StringBuilder();
	   	sb.append("[");
	   	/*
		 * Append the Data from the Asset-Channels to the DataMessage
		 */
		for (Map.Entry<String, OMFAsset> entry : listOfAssets.entrySet()){
           	 OMFAsset asset = entry.getValue();      		 	 
		 	 for (Map.Entry<String, OMFChannel> channelEntry : asset.getChannels().entrySet()){	 		 	 
		 		 OMFChannel channel = channelEntry.getValue();
		 		 sb.append(channel.getDataMessageJSON()).append(",");
	         } //for	 
         } //for	 
		if(sb.length() > 1) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("]");
		return sb.toString();
	}
	
	/**
	 * Generates the assets and links to a string in JSON format.
	 * @param listOfAssets key-value-Store of every created asset
	 * @return JSON formatted string of assets and links
	 */
	public String createAssetsAndLinks(Map<String, OMFAsset> listOfAssets){
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(getFirstAFElement()).append(",");
		
		//Creates a root element (host name) under which all assets are arranged		
		for (Map.Entry<String, OMFAsset> entry : listOfAssets.entrySet()){
        	 OMFAsset asset = entry.getValue();    	
        	/*
        	 * Append the Assets as Links to the DataMessage
        	 */
	 		sb.append(asset.getDataMessageJSON());
	 		sb.append(",");
         } //for		
		if(sb.length() > 1) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("]");
		return sb.toString();
	}
	
	/**
	 * Creates the first AF element (the name of the IoT-Device)
	 * @return JSON-formatted String
	 */
	public String getFirstAFElement() {
		Gson gson = new Gson();
		StringBuilder sb = new StringBuilder();
		String jsonAFLink = "";
		String jsonAFElement = "";
		HashMap<String, String> listValues = new HashMap<>();
		HashMap<String, String> properties = new HashMap<>();
		ArrayList<HashMap<String, String>> afInformation = new ArrayList<>();
		/*
		 * build the DataMessage of the default IoTDevice. 
		 * These are the Properties which were defined in the TypeMessage before
		 */
		String propertyId = "Name";
		properties.put(propertyId, this.omfPublisherOptions.getDevicename());
		listValues.put("Name", this.omfPublisherOptions.getDevicename());
		
		afInformation.add(listValues);
		AssetFrameworkElement afElement = new AssetFrameworkElement("KuraIoTDevice", afInformation);
		/*
		 * build the DataMessage for the Root Asset Link
		 */
		SourceTarget source = new SourceTarget("KuraIoTDevice", "_ROOT");
		SourceTarget target = new SourceTarget("KuraIoTDevice", this.omfPublisherOptions.getDevicename());
		ArrayList<LinkedValues> linkedValues = new ArrayList<>();
		
		linkedValues.add(new LinkedValues(source, target));
		LinkedDataMessage afElementLinkRoot = new LinkedDataMessage("__Link", linkedValues);
		/*
		 * parse the Objects into JSON and append them
		 */
		jsonAFElement = gson.toJson(afElement);
		jsonAFLink = gson.toJson(afElementLinkRoot);
		
		sb.append(jsonAFElement).append(",").append(jsonAFLink);
		return sb.toString();
	}
	
	/**
	 * Send a request to the appropriate TargetURL. The request depends on the type of the message:
	 * Type, Container, Data
	 * It is necessary to send a header and its body (HTTP).
	 * @param action Action of the message (create, update, delete). If omitted "create" is set
	 * @param messageType Type of the message (type, container, data)
	 * @param messageJson Body of the message in JSON
	 * @throws SocketTimeOutException if a connection to a socket timed out
	 * @throws IOException if an I/O error occurs.
	 */
	public synchronized int sendOMFMessageRequestToHost(String action, String messageType, String messageJson) {
		int status = 404;
	    try {	
	    	byte[] compressedMessage = compressMessage(messageJson);

	    	logger.debug("Size after compression: "+compressedMessage.length+" byte");
			/*
	    	 * define HTTP connection with the request method and calculation of the body-size
	    	 */
	        URL requestURL = new URL(omfPublisherOptions.getTargetURL());
	     
	    	HttpsURLConnection myRequest = (HttpsURLConnection)requestURL.openConnection();
		    myRequest.setRequestMethod("POST");
		    myRequest.setDoOutput(true);
		    myRequest.setFixedLengthStreamingMode(compressedMessage.length);
		    
	    	//set HTTP-header
		    myRequest.setRequestProperty("producertoken", omfPublisherOptions.getProducerToken());
		    myRequest.setRequestProperty("messagetype", messageType);
		    myRequest.setRequestProperty("action", action);
		    myRequest.setRequestProperty("messageformat", "JSON");
		    myRequest.setRequestProperty("omfversion", "1.0");
		    myRequest.setRequestProperty("compression", "gzip");
		    
		    logger.debug("Message Type: <"+messageType+"> Send message: " + messageJson);
	        // Send the request, and collect the response
	        myRequest.connect();
	        try(OutputStream os = myRequest.getOutputStream()) {
	            os.write(compressedMessage);
	        } //try
	        status = myRequest.getResponseCode();
			logger.info("Message response code: " + myRequest.getResponseCode());
			if (status > 204){
				logger.error("Relay returned error code"+myRequest.getResponseCode());
				InputStream in =myRequest.getErrorStream();
				String result = IOUtils.toString(in, StandardCharsets.UTF_8);			
				
				logger.error("response was:"+ result);			
				logger.error("Message was: " + messageJson);
			} //if
			return status;
	    } catch (SocketTimeoutException stoex) {
	    	// Log SocketTimeout error, if it occurs
	    	logger.error(" Error during web request [SocketTimeout]: " + stoex.getMessage());
	    	return status;
	    } catch (IOException ex) {
	        // Log any error, if it occurs
	    	logger.error(" Error during web request: " + ex.getMessage());
	    	return status;
	    }
	}
	
	/**
	 * Compress the JSON-Message with GZIP
	 * @param message
	 * @return byte-Array which is compressed with GZIP or an triggered Exception
	 * @throws IOException if an I/O error occurs.
	 */
	 public byte[] compressMessage(String message) {
	        if ((message == null) || (message.length() == 0)) {
	            throw new IllegalArgumentException("Cannot zip null or empty string");
	        } //if
	        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
	            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
	                gzipOutputStream.write(message.getBytes(StandardCharsets.UTF_8));
	            } //try
	            return byteArrayOutputStream.toByteArray();
	        } catch(IOException e) {
	        	logger.error(" Error during GZIP-Compression: " + e.getMessage());
	            throw new RuntimeException("Failed to zip content", e);
	        } //catch
	    }
	 
	 /**
	  * Checks the size of an OMF message and returns if it's bigger than 192kb
	  * @param message
	  * @return true or false
	  */
	 public boolean isMessageSizeTooLarge(String message) {
		//Check if the compressed message-size is bigger than 192kb    	
		 return message.getBytes().length >= (192*1000);
	 }	
}
