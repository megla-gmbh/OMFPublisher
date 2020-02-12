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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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
import org.eclipse.kura.ssl.SslManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.megla.iot.OMFPublisher.models.AssetFrameworkElement;
import de.megla.iot.OMFPublisher.models.LinkedDataMessage;
import de.megla.iot.OMFPublisher.models.LinkedValues;
import de.megla.iot.OMFPublisher.models.OMFAsset;
import de.megla.iot.OMFPublisher.models.OMFAssetList;
import de.megla.iot.OMFPublisher.models.OMFChannel;
import de.megla.iot.OMFPublisher.models.OMFTypeMessage;
import de.megla.iot.OMFPublisher.models.Property;
import de.megla.iot.OMFPublisher.models.SourceTarget;

/**
 * OMFPublisherService.java
 * 
 * The OMFPublisherService class supports the publisher by writing the individual definitions of Type, Container, 
 * and Data in a suitable JSON format. In addition, it creates the header according to the "messagetype" 
 * and makes a request to the destination system.
 */

public class OMFPublisherService{
	/**
	 * <b>omfPublisherOptions</b> contains option-data for example the producertoken, hostname, targeturl
	 * <b>logger</b> logs all messages for Debugging (Info, Warning, Error)
	 * <b>sslManagerService</b> Service for setting the SSL connection settings
	 */ 
	private OMFPublisherOptions omfPublisherOptions;
	private static final Logger logger = LoggerFactory.getLogger(OMFPublisherService.class);
	private SslManagerService sslManagerService;
	
	/**
	 * Constructor in which the options are set and the SSL certificates are validated.
	 */
	OMFPublisherService(OMFPublisherOptions myPublisherOptions, SslManagerService sslManagerService){
		logger.debug("Initializing OMFpublisherHelper");
		
		this.omfPublisherOptions=myPublisherOptions;
		this.sslManagerService = sslManagerService;
		
		if (this.omfPublisherOptions.getSSLVerify())
			trustOnlyKnownCertificates();		
		else
			trustAllConnections();
	}
	
	/**
	 * Generates a JSON string for the type message, every asset, and their channels.
	 */
	public String createTypeMessage(OMFAssetList listOfAssets) {
		Gson gson = new Gson();
		StringArrayBuilder arrayBuilder = new StringArrayBuilder();
		Map<String, Property> properties = new HashMap<>();
		
		Property propName = new Property("string", true);
		properties.put("Name", propName);
		
		OMFTypeMessage typeMessage = new OMFTypeMessage("KuraIoTDevice", null, "object", "static", properties, null);
		
		arrayBuilder.addContent(gson.toJson(typeMessage));
		
		for (OMFAsset asset : listOfAssets){
			
        	 arrayBuilder.addContent(asset.getTypeMessageJSON());
		 	 
        	 for (Map.Entry<String, OMFChannel> channelEntry : asset.getChannels().entrySet())
		 		 arrayBuilder.addContent(channelEntry.getValue().getTypeMessageJSON());
        }
		
		return arrayBuilder.getArrayString();
	}
	
	/**
	 * Generates the containers of all channels in all assets.
	 */
	public String createContainerMessage(OMFAssetList assetList){
		StringArrayBuilder arrayBuilder = new StringArrayBuilder();
		 
		for (OMFAsset asset : assetList)
			for (Map.Entry<String, OMFChannel> channelEntry : asset.getChannels().entrySet())	
		 		arrayBuilder.addContent(channelEntry.getValue().getContainerMessageJSON());
		
		return arrayBuilder.getArrayString();
	}
	
	/**
	 * Creates the JSON data for a data container. Appends and returns all channel data.
	 */
	public String createDataValuesMessage(OMFAssetList assetList) {
		StringArrayBuilder arrayBuilder = new StringArrayBuilder();
		
	   	for (OMFAsset asset : assetList)
           	 for (Map.Entry<String, OMFChannel> channelEntry : asset.getChannels().entrySet()) 		 
		 		 arrayBuilder.addContent(channelEntry.getValue().getDataMessageJSON());
		
		return arrayBuilder.getArrayString();
	}
	
	/**
	 * Generates the assets and links to a string in JSON format.
	 */
	public String createAssetsAndLinks(OMFAssetList assetList){
		StringArrayBuilder arrayBuilder = new StringArrayBuilder();
		arrayBuilder.addContent(createRootElement());
				
		for (OMFAsset asset : assetList)
	 		arrayBuilder.addContent(asset.getDataMessageJSON());
		
		return arrayBuilder.getArrayString();
	}
	
	/**
	 * Creates the first AF element (the name of the IoT-Device).
	 */
	public String createRootElement() {
		
		Gson gson = new Gson();
		StringBuilder sb = new StringBuilder();
		String jsonAFLink = "";
		String jsonAFElement = "";
		String propertyId = "Name";
		HashMap<String, String> listValues = new HashMap<>();
		HashMap<String, String> properties = new HashMap<>();
		ArrayList<HashMap<String, String>> afInformation = new ArrayList<>();
		
		
		//build the DataMessage of the default IoTDevice. 
		//These are the Properties which were defined in the TypeMessage before
		properties.put(propertyId, this.omfPublisherOptions.getDevicename());
		listValues.put("Name", this.omfPublisherOptions.getDevicename());
		
		afInformation.add(listValues);
		AssetFrameworkElement afElement = new AssetFrameworkElement("KuraIoTDevice", afInformation);
		
		//build the DataMessage for the Root Asset Link
		SourceTarget source = new SourceTarget("KuraIoTDevice", "_ROOT");
		SourceTarget target = new SourceTarget("KuraIoTDevice", this.omfPublisherOptions.getDevicename());
		ArrayList<LinkedValues> linkedValues = new ArrayList<>();
		
		linkedValues.add(new LinkedValues(source, target));
		LinkedDataMessage afElementLinkRoot = new LinkedDataMessage("__Link", linkedValues);
		
		//parse the Objects into JSON and append them
		jsonAFElement = gson.toJson(afElement);
		jsonAFLink = gson.toJson(afElementLinkRoot);
		
		sb.append(jsonAFElement).append(",").append(jsonAFLink);
		
		return sb.toString();
	}
	
	/**
	 * Send a request to the appropriate TargetURL. The request depends on the type of the message:
	 * Type, Container, Data
	 * It is necessary to send a header and its body (HTTP).
	 * @param action Action of the message (create, update, delete). If omitted "create" is set.
	 * @param messageType Type of the message (type, container, data)
	 */
	public synchronized int handleOMFMessageRequest(String action, String messageType, String messageJson) {
		int status = 404;
	    
		try {
	    	HttpsURLConnection response = sendOMFMessage(action, messageType, messageJson);
	        
	        status = response.getResponseCode();
			logger.info(String.format("Message response: %d - %s", status, response.getResponseMessage()));
			
			if (status > 204){
				InputStream in =response.getErrorStream();
				String result = IOUtils.toString(in, StandardCharsets.UTF_8);			
				
				ErrorHandling.handle(logger, String.format("Relay returned error code %d", status)
						, String.format("response was: %s", result)
						, String.format("Message was: %s", messageJson));
			}
			
	    } catch (IOException  ex) {
	    	ErrorHandling.handle(" Error during web request: ", ex, logger);
	    }
		
		return status;
	}

	/**
	 * Sends a OMF message to the OMF target.
	 */
	public HttpsURLConnection sendOMFMessage(String action, String messageType, String messageJson) {
		HttpsURLConnection result = null;
		int timeout = this.omfPublisherOptions.getConnectionTimeout() * 1000;
		
		try {
			byte[] compressedMessage = compressMessage(messageJson);

	    	logger.debug(String.format("Size after compression: %d byte", compressedMessage.length));
	    	
	    	//define HTTP connection with the request method and calculation of the body-size
	        URL requestURL = new URL(omfPublisherOptions.getTargetURL());
		
			result = (HttpsURLConnection)requestURL.openConnection();
			
			result.setConnectTimeout(timeout);
			result.setReadTimeout(timeout);
			result.setRequestMethod("POST");
			result.setDoOutput(true);
			result.setFixedLengthStreamingMode(compressedMessage.length);
			
			//set HTTP-header
			result.setRequestProperty("producertoken", omfPublisherOptions.getProducerToken());
			result.setRequestProperty("messagetype", messageType);
			result.setRequestProperty("action", action);
			result.setRequestProperty("messageformat", "JSON");
			result.setRequestProperty("omfversion", "1.0");
			result.setRequestProperty("compression", "gzip");
			
			// Send the request, and collect the response
	        result.connect();
	        
	        try(OutputStream os = result.getOutputStream()) {
	            os.write(compressedMessage);
	        }
			
			logger.debug(String.format("Message Type: <%s> Send message: %s", messageType,messageJson));
		}catch (IOException ex) {
			ErrorHandling.handle("Error during request generation.", ex, logger);
		}
		
		return result;
	}
	
	/**
	 * Compress the JSON-Message with GZIP
	*/
	public byte[] compressMessage(String message) {
	        
		if ((message == null) || (message.length() == 0))
			throw new IllegalArgumentException("Cannot zip null or empty string");
	    
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
	        
			try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
	            gzipOutputStream.write(message.getBytes(StandardCharsets.UTF_8));
	        }
			
	        return byteArrayOutputStream.toByteArray();
	    } catch(IOException ex) {
	    	ErrorHandling.handle("Error during GZIP-Compression: ", ex, logger);
	        throw new RuntimeException("Failed to zip content", ex);
	    }
	}
 
	/**
	 * Trust only certificates in the Kura keystore.
	 */
	private void trustOnlyKnownCertificates() {
		try {
			HttpsURLConnection.setDefaultSSLSocketFactory(this.sslManagerService.getSSLSocketFactory());
		} catch (IOException | GeneralSecurityException e) {
			ErrorHandling.handle("Error while setting the default ssl factory." , e, logger);
		}
	}

	/**
	 * Configures the HttpsURLConnection to accept all connection and certificates.
	 */
	private void trustAllConnections() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = createTrustManager();	 
		// Install the all-trusting trust manager
		SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");
		    sc.init(null, trustAllCerts, new java.security.SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		       
		    HostnameVerifier allHostsValid = createHostnameVerifier();
		    
		    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		    
		} catch (NoSuchAlgorithmException | KeyManagementException ex) {
			ErrorHandling.handle("Error disabling certifficate validation: " , ex, logger);
		}
	}

	/**
	 * Creates a hostnameverifier which does verifies always true. 
	 */
	private HostnameVerifier createHostnameVerifier() {
		return new HostnameVerifier() {
		    @Override
			public boolean verify(String hostname, SSLSession session) {
		        return true;
		    }
		};
	}

	/**
	 * Creates a TrustManager which does not validate certificates.
	 */
	private TrustManager[] createTrustManager() {
		return new TrustManager[] {new X509TrustManager() {
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
	}  
}

