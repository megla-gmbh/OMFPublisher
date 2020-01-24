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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.internal.wire.asset.WireAssetConstants;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.megla.iot.OMFPublisher.models.OMFAsset;
import de.megla.iot.OMFPublisher.models.OMFChannel;

/**
 * OMFPublisher.java
 * 
 * The Class de.megla.iot.OMFPublisher is a specific Wire Component to publish a list of
 * {@link WireRecord}s as received in {@link WireEnvelope} to a destination system
 * platform.
 * For every {@link WireRecord} as found in {@link WireEnvelope} will be wrapped inside a OMF Payload
 */
@Component(	property="service.pid=de.megla.iot.OMFPublisher.OMFPublisher",
					service={ConfigurableComponent.class, WireComponent.class,
							WireReceiver.class, Consumer.class},
					configurationPolicy=ConfigurationPolicy.REQUIRE
)
@Designate(ocd=OMFPublisherConfig.class, factory=true)
public final class OMFPublisher implements ConfigurableComponent, WireReceiver{

	/**
	 * <b>logger</b> 					logs all messages for Debugging (Info, Warning, Error)
	 * <b>omfPublisherOptions</b> 		contains option-data for example the producertoken, hostname, targeturl
	 * <b>omfPublisherHelper</b> 		provides the ability to write individual definitions of Type, Container and Data 
	 * 									in a suitable JSON format. In addition, it creates the header according to the "messagetype" 
	 * 									and makes a request to the destination system
	 * <b>wireHelperService</b> 		Interface is used as a utility API to provide fast and necessary operations for the Kura Wires.
	 * <b>wireSupport</b>				Interface for managing incoming and outgoing wire components. 
	 * 									Additionally for use, e.g. To send and receive wireRecords.
	 * <b>knownAssetList</b>			Key-Value store of the last known Assets and their Channels
	 * <b>properties</b>				properties which contain service configurations and user specific inputs
	 * <b>inFlightMessages</b>			List of all unpublished Messages as an in-memory datacaching
	 * <b>executorServiceInFlight</b> 	Scheduled Thread, which sends all cached Messages to a connector
	 */
    private static final Logger logger =  LoggerFactory.getLogger(OMFPublisher.class);
    
    private OMFPublisherOptions omfPublisherOptions;
    private OMFPublisherHelper omfPublisherHelper;
    
    private WireHelperService wireHelperService;
    private WireSupport wireSupport;
    
    private Map<String, OMFAsset> knownAssetList;
    private Map<String, Object> properties;
    
    private List<Map<String, OMFAsset>> inFlightMessages;
    private ScheduledExecutorService executorServiceInFlight;    

    //Keys to get information from the wireRecords
    private static final String ASSET_NAME_PROPERTY_KEY = WireAssetConstants.PROP_ASSET_NAME.value().toString();
    private static final String SINGLE_TIMESTAMP_NAME = WireAssetConstants.PROP_SINGLE_TIMESTAMP_NAME.value().toString();
    private static final String SUFFIX_TIMESTAMP = WireAssetConstants.PROP_SUFFIX_TIMESTAMP.value().toString();
    
    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService the new Wire Helper Service       
     */
    @Reference(unbind="unbindWireHelperService")
    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }//if
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService the new Wire Helper Service
     */
    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }//if
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    /**
     * OSGi Service Component callback for activation.
     *
     * @param componentContext the component context       
     * @param properties the properties          
     */
    @Activate
    protected void activate( final Map<String, Object> properties) {   
    	try {
	        logger.info("Activating OMF Publisher Wire Component...");
	        this.wireSupport = this.wireHelperService.newWireSupport(this); 
		    this.properties = properties;
	        updated(this.properties);
	        
	        this.omfPublisherHelper = new OMFPublisherHelper(omfPublisherOptions);
	        this.knownAssetList=new HashMap<>();
	        this.inFlightMessages = new ArrayList<>();
	        
	        executorServiceInFlight = Executors.newScheduledThreadPool(1);
	        sendInFlightMessagesTask();

	        logger.info("Activating OMF Publisher Wire Component... Done");
    	}catch(NullPointerException e){
 			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);		
			logger.error(String.format("Error in activating OMF Publisher Wire Component  %n %s %n %s", e.getMessage(), stringWriter.toString()));
 		} catch(Exception e) {
 			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);		
			logger.error(String.format("Error in activating OMF Publisher Wire Component  %n %s %n %s", e.getMessage(), stringWriter.toString()));
 		}
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties the updated properties       
     */
    @Modified
    public void updated(final Map<String, Object> properties) {
        logger.info("Updating OMF Publisher Wire Component...");
        
        this.properties = properties;
       
        //compare Options if something changed
        if(nonNull(this.omfPublisherOptions)) {
        	OMFPublisherOptions options = new OMFPublisherOptions(this.properties);
        	if(!this.omfPublisherOptions.compare(options)){
        		// Update properties
            	this.omfPublisherOptions = new OMFPublisherOptions(this.properties);
            	
            	if(!this.executorServiceInFlight.isShutdown()) {
                	this.executorServiceInFlight.shutdown();
                } // if
            	// Start Thread for sending in flight messages
                sendInFlightMessagesTask();      
        	} 
        	else if (nonNull(this.knownAssetList)) { 
        		//Remove all known assets in order to send definitions again.
               	 this.knownAssetList.clear();
              } //else if 
        }else{ 
        	this.omfPublisherOptions = new OMFPublisherOptions(this.properties);
        } //else 
        logger.info("Updating OMF Publisher Wire Component... Done");
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext the component context
     */
    @Deactivate
    protected void deactivate() {
        logger.info("Deactivating OMF Publisher Wire Component...");
        if(executorServiceInFlight != null) {
        	executorServiceInFlight.shutdown();
        } //if
        logger.info("Deactivating OMF Publisher Wire Component... Done");
    }

    /** 
	 * Listener is called, when an WireEnvelope is received
     */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");

        if (nonNull(this.omfPublisherOptions)) {
            final List<WireRecord> records = wireEnvelope.getRecords();
            publish(records);
        }//if
    }

    /**
     * Inherited method from interface org.osgi.service.wireadmin.Consumer
     */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /**
     * Inherited method from interface org.osgi.service.wireadmin.Consumer
     */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------
    
    /**
     * Publishes the list of provided {@link WireRecord}s
     *
     * @param wireRecords the provided list of {@link WireRecord}s
     * @throws NullPointerException if one of the arguments is null          
     */
    private void publish(final List<WireRecord> records) {
    	logger.info("Publishing Message...");
        requireNonNull(records, "Wire Records cannot be null");  
        Map<String, OMFAsset> currentAssetList = createMapWithAssetsAndChannels(records); 
        
        try {    
			synchronized(this.inFlightMessages) {
				this.inFlightMessages.add(currentAssetList);
			}				
        }catch(NullPointerException e) {
        	logger.error("Error in publishing wire records using PIOMF publisher..", e);
        }
    }
    
    // ----------------------------------------------------------------
    //
    // Public methods
    //
    // ----------------------------------------------------------------    

	/**
     * Method creates a full List (Map) of ever Asset and their Channels and return that Map.
     * @param wireRecords list of the wire records to get the properties out of it and create the Asset-list
     * @return key-value-Map with every Asset and their Channels
     * @throws NullPointerException if one of the arguments is null  
     */
    public Map<String, OMFAsset> createMapWithAssetsAndChannels(final List<WireRecord> wireRecords){
    	//Instantiate asset list for rebuilt
    	Map<String, OMFAsset> currentAssetList= new HashMap<>();
    	try {
       	    //Analyze wireRecord and build asset and channel structure
    	    for (final WireRecord dataRecord : wireRecords) {
    	    	Map<String, TypedValue<?>> wireRecordProps = dataRecord.getProperties();
    	    	//Check if assetname exists, if not throw exception
    	    	
    	    	if(!wireRecordProps.containsKey(ASSET_NAME_PROPERTY_KEY)){
    	    		throw new NullPointerException("Wirerecord does not contain assetname information");
    	    	} //if
    	    	//add asset to new list
    	    	String assetname = (String)dataRecord.getProperties().get(ASSET_NAME_PROPERTY_KEY).getValue();
    	    	assetname = removeSpecialCharacters(assetname);
    	    	if(!currentAssetList.containsKey(assetname)){
    	 			OMFAsset newasset=new OMFAsset(assetname,this.omfPublisherOptions);
    	 			currentAssetList.put(assetname,newasset);
    	 		} //if
    	    	OMFAsset asset = currentAssetList.get(assetname);
    	    	//check if the assetTimestamp is available
    	    	if(wireRecordProps.containsKey(SINGLE_TIMESTAMP_NAME)){
    	    		//Go through all the entries and extract the channels
    	    		for (Map.Entry<String, TypedValue<?>> entry : wireRecordProps.entrySet()) {
    	    			
        	            String key=entry.getKey();
        	        	if(!(key.equals(SINGLE_TIMESTAMP_NAME) || key.equals(ASSET_NAME_PROPERTY_KEY))){
        	        		
        	        		// if there is value which is not serializable with JSON jump over this entry
        	    			if(isSpecialFloatingPointValue(entry.getValue())) {
        	    				continue;
        	    			}
        	        		
        	        		String channelname=key;
        	        		channelname = removeSpecialCharacters(channelname);
        			 		//Add channel if it is not already created
        			 		if(!asset.getChannels().containsKey(channelname)){
        			 			OMFChannel newchannel=new OMFChannel(channelname,this.omfPublisherOptions, asset);
        			 			asset.getChannels().put(channelname, newchannel);
        			 		} //if
        			 		OMFChannel channel=asset.getChannels().get(channelname);
        			 		TypedValue<?> typedTimestamp = dataRecord.getProperties().get(SINGLE_TIMESTAMP_NAME);			 		
        			 		channel.setTimestamp(new Date((long)typedTimestamp.getValue()));
        			 		channel.setTypedValue(dataRecord.getProperties().get(key));	
        			    } //if 
        	        	
        	        } //for      	    		
      	    	}
    	    	else{ //if there is no assetTimestamp is available
    	    		for (Map.Entry<String, TypedValue<?>> entry : wireRecordProps.entrySet()){
    	    			String key=entry.getKey();
    	    			if(key.contains(SUFFIX_TIMESTAMP)){
    	    				
    	    				// if there is value which is not serializable with JSON jump over this entry
        	    			if(isSpecialFloatingPointValue(entry.getValue())) {
        	    				continue;
        	    			}
    	    				
    	    				String channelname=key.replace(SUFFIX_TIMESTAMP, "");
    	    				channelname = removeSpecialCharacters(channelname);
    	    				//Add channel if it is not already created
        			 		if(!asset.getChannels().containsKey(channelname)){
        			 			OMFChannel newchannel=new OMFChannel(channelname,this.omfPublisherOptions, asset);
        			 			asset.getChannels().put(channelname, newchannel);
        			 		}
        			 		OMFChannel channel=asset.getChannels().get(channelname);
        			 		TypedValue<?> typedTimestamp= dataRecord.getProperties().get(key);
        			 		channel.setTimestamp(new Date((long)typedTimestamp.getValue()));	
        			 		channel.setTypedValue(dataRecord.getProperties().get(channelname));	
    	    			} //if
        	        } //for  
    	    	} //else    	
    	    }//for
    	}catch(NullPointerException e) {
    		 logger.error("Error in creating Asset-List with wire records using PIOMF publisher..", e);
    	} //catch
    	return currentAssetList;
    }
    
    /**
     * checks if an Asset or Channel was modified in Wire and put it into the knownAssetList
     * @param currentAssetList
     * @return true if an Asset or Channel was modified in Wire or false if not
     */
    public boolean isAssetOrChannelModified(Map<String, OMFAsset> currentAssetList) {
        //Check if types and containers are all included as they have been sent before
        boolean isModified =false;
        
        //Check asset and if it is not already in the map set the flag
        for (Map.Entry<String, OMFAsset> entry : currentAssetList.entrySet()) {
	       	String assetname=entry.getKey();
	       	assetname = removeSpecialCharacters(assetname);
	       	OMFAsset asset = entry.getValue();
	       	if(this.knownAssetList == null) {
	       		this.knownAssetList= new HashMap<>();
	       	} //if
	       	if(!this.knownAssetList.containsKey(assetname) || this.knownAssetList.isEmpty()){
	       		isModified=true;
	       		logger.debug("New Asset "+assetname+ " detected");
		 		OMFAsset newAsset=new OMFAsset(assetname,this.omfPublisherOptions);
		 		this.knownAssetList.put(assetname,newAsset);
			} //if
			//Check channel and add it to the map, if it is not already in the map set the flag
			for (Map.Entry<String, OMFChannel> channelEntry : asset.getChannels().entrySet()){
			 	String channelname=channelEntry.getKey();
			 	channelname = removeSpecialCharacters(channelname);
			 	OMFChannel channel = channelEntry.getValue();
			 	if(!this.knownAssetList.get(assetname).getChannels().containsKey(channelname) || 
			 		this.knownAssetList.get(assetname).getChannels().get(channelname).getTypedValue().getType() != channel.getTypedValue().getType()) {
			 		isModified=true;
			 		logger.debug("New Channel "+channelname+" detected");
			 		OMFChannel newChannel=new OMFChannel(channelname,this.omfPublisherOptions, asset);	
			 		newChannel.setTypedValue(TypedValues.newTypedValue(channel.getTypedValue().getValue()));
			 		this.knownAssetList.get(assetname).getChannels().put(channelname, newChannel);
			 	} //if
		    }//for	 
        }//for
        return isModified;
    }
    
    /**
     * Sends the message, of a modified Asset or Channel to the destination system with a new type-, container and data definition
     * @throws OutOfMemoryError if the RAM has no space for more messages  
     */
    public void sendModifiedMessage() {
    	int status;
    	boolean connected = true;
    	
    	status = sendTypeMessage(this.knownAssetList);
		if(!isStatusCodeOk(status)) {
			printOMFFailure(status);
			connected = false;						
		}else {
			logger.info("Sent <Type> correctly..." );
		} //else
		status = sendContainerMessage(this.knownAssetList);
		if(!isStatusCodeOk(status)) {
			printOMFFailure(status);
			connected = false;						
		}else {
			logger.info("Sent <Container> correctly...");
		} //else
		status = sendDataMessageLinks(this.knownAssetList);
		if(!isStatusCodeOk(status) || !connected ) {
			printOMFFailure(status);
			/*
			 * save as in-flight messages
			 */
			try {
				synchronized(this.inFlightMessages) {
					this.inFlightMessages.add(this.knownAssetList);
				} //synchronized
			}catch(OutOfMemoryError ex) {
				logger.error("The RAM is out of memory! Incoming message are not saved!" + ex);
			} //catch
			
		} //if
		else {
			logger.info("Sent <Data Links> correctly...");	
		} //else
    }
    
    /**
     * sends the first in-flight message to the destination system. If there is something wrong with the connection or message, 
     * put it back at the end of the list. In the end remove the first list entry of in-flight messages.
     * @throws Exception if there goes something wrong with sending the message
     * @throws OutOfMemoryError if the RAM has no space for more messages  
     */
    public synchronized void sendInFlightMessage() {
    	logger.info("In-flight Message will be send...");
    	int status;
    	Map<String, OMFAsset> inFlightAssetList = this.inFlightMessages.get(0);
    	try {
    		if(isAssetOrChannelModified(inFlightAssetList)) {
        		this.sendModifiedMessage();
        	} //if
        	status = sendDataMessage(inFlightAssetList);

        	if(isStatusCodeOk(status)) {
        		logger.info("Sent in-flight <Data> correctly...");
        		logger.info("Sent in-flight Message...");
        		synchronized(this.inFlightMessages) {
        			this.inFlightMessages.remove(0);    		
        		} //synchronized
        	}else {
        		synchronized(this.inFlightMessages) {
        			this.inFlightMessages.add(inFlightAssetList);	
        		} //synchronized	
        	} //else
    	}catch(Exception ex) {
    		logger.error(" Error during sending In Flight Messages: " + ex.getMessage());
    	} catch(OutOfMemoryError ex) {
			logger.error("The RAM is out of memory! Incoming message are not saved! " + ex);
		}	//catch		
    } 

    /**
     * sends the Type Message for all known assets and return the HTTP-Status from the response
     * @param assets
     * @return HTTP-Status from response
     */
    public int sendTypeMessage(Map<String, OMFAsset> assets) {
    	//Generate the type from all assets and channels
    	String typeDefinitionJSON= this.omfPublisherHelper.createTypeMessage(assets); 
	    int status = this.omfPublisherHelper.sendOMFMessageRequestToHost("create", "type", typeDefinitionJSON);
	    logger.info("sendTypeMessage() - StatusCode: "+ status);
	    return status;
    }
    
    /**
     * sends the Container Message for all known assets and return the HTTP-Status from the response
     * @param assets
     * @return HTTP-Status from response
     */
    public int sendContainerMessage(Map<String, OMFAsset> assets) {
    	//Generate containers from all assets and channels
	    String containerDefinitionJSON= this.omfPublisherHelper.createContainerMessage(assets);
	    int status = this.omfPublisherHelper.sendOMFMessageRequestToHost("create", "container", containerDefinitionJSON);
	    logger.info("sendContainerMessage() - StatusCode: " + status);
	    return status;
    }
    
    /**
     * sends the Data Message for all known assets and their links and return the HTTP-Status from the response
     * @param assets
     * @return HTTP-Status from response
     */
    public int sendDataMessageLinks(Map<String, OMFAsset> assets) {
    	//Create assets and links	  
		String assetandlinksJSON = this.omfPublisherHelper.createAssetsAndLinks(assets);
		int status = this.omfPublisherHelper.sendOMFMessageRequestToHost("create", "data", assetandlinksJSON);	
	    logger.info("sendDataMessageLinks() - StatusCode: "+ status);
	    return status;
    }
    
    /**
     * sends the Data Message for all known assets and return the HTTP-Status from the response
     * @param assets
     * @return HTTP-Status from response
     */
    public int sendDataMessage(Map<String, OMFAsset> assets) {
    	// If ready, send finished JSON with the containers and payload
    	// only from the currently received wires, hence currentAssetList
    	String dataJSON= this.omfPublisherHelper.createDataValuesMessage(assets);
    	int status = -1;
    	// Send the JSON message to the target URL
    	status = this.omfPublisherHelper.sendOMFMessageRequestToHost("create", "data", dataJSON);
    	
    	logger.info("sendDataMessage() - StatusCode: "+ status);
    	return status;
    }
    
    /**
     * checks if the responsed http-status-code is a good response (OK, ACCEPTED, NO CONTENT)
     * @param status of an HTTP-response
     * @return true or false
     */
    public boolean isStatusCodeOk(int status) {
    	return status == HttpStatusCode.NOCONTENT.getStatus() || 
    		status == HttpStatusCode.ACCEPTED.getStatus() || 
    		status == HttpStatusCode.OK.getStatus() || 
    	    status == HttpStatusCode.BADREQUEST.getStatus();
    }
    
    /**
     * removes all not allowed special characters from Asset- and Channelname and cuts the String if more than 60 Characters
     * @param input
     * @return String without not allowed characters and a length of at he most 60 characters
     */
    public String removeSpecialCharacters(String input) {		 
    	String result = input.replaceAll("[\\[\\]\\|!?\\\\;`´{}()'*]", ""); 
    	if (result.length() >=60) {
    		result = cutString(result);
    	} //if
    	return result;
    }
    
    /**
     * Starts a Thread that checks if there are saved in-flight messages. When the publisher is connected with the destination system
     * again, send every in-flight message
     */
    public void sendInFlightMessagesTask() {
    	final int inFlightInterval = this.omfPublisherOptions.getinFlightInterval();
    	logger.info("Start Task to sending in-flight messages...");
    	/*
    	 * Runnable Task to send in-flight messages if possible  
    	 */
		Runnable runnableTaskSendMessage = () -> {
			
			if(!inFlightMessages.isEmpty() && isStatusCodeOk(isConnectionToPiServerEstablished())) {
				/*
			     * send every in-flight message one by one every x seconds
			     */
				sendInFlightMessage();
			} //if
		};
		executorServiceInFlight.scheduleWithFixedDelay(runnableTaskSendMessage, 100, inFlightInterval, TimeUnit.MILLISECONDS);
    }

	/**
	 * send HTTP request to the  Connector for checking if a connection ist established
	 * @param newUrl url of the Connector, could be edited at runtime --> get the new url
	 * @return status code of the response
	 * @throws MalformedURLException if either no legal protocol could be found in a specification string or the string could not be parsed.
	 * @throws IOException  if an I/O error occurs.
	 */
	public int isConnectionToPiServerEstablished() {
		 int timeout = this.omfPublisherOptions.getConnectionTimeout() * 1000;
		 int status = 404;
		 try {
			URL url = new URL(this.omfPublisherOptions.getTargetURL());
			HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
			
			urlConnection.setConnectTimeout(timeout);
			urlConnection.setReadTimeout(timeout);
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			
			urlConnection.setRequestProperty("producertoken", omfPublisherOptions.getProducerToken());
			urlConnection.setRequestProperty("messagetype", "type");
			urlConnection.setRequestProperty("action", "create");
			urlConnection.setRequestProperty("messageformat", "JSON");
			urlConnection.setRequestProperty("omfversion", "1.0");
			
		    urlConnection.connect();
		   
		    try(OutputStream os = urlConnection.getOutputStream()) {
	            os.write("[]".getBytes());
	        } //try
		    return urlConnection.getResponseCode();
		} catch (MalformedURLException e) {
			logger.error("Error URL: "+e);
			return status;
		} catch (IOException e) {
			logger.error("Error Connecting to url: "+e);
			return status;
		} 
	 }

	/**
     * prints the OMF Status Message if there is something wrong with the HTTP request
     */
    public void printOMFFailure(int status) {
    	if(status == HttpStatusCode.BADREQUEST.getStatus()) {
    		logger.error("The OMF message was malformed or not understood. The client should not retry sending the message without modifications.");
    	} //if
    	else if(status == HttpStatusCode.UNAUTHORIZED.getStatus()) {
    		logger.error("Authentication failed.");
    	} //else if
    	else if(status == HttpStatusCode.FORBIDDEN.getStatus()) {
    		logger.error("Authentication succeeded, but not authorized.");
    	}//else if
    	else if(status == HttpStatusCode.PAYLOADTOOLARGE.getStatus()) {
    		logger.error("Payload size exceeds OMF body size limit of 192kb.");
    	}//else if
    	else if(status == HttpStatusCode.INTERNALSERVERERROR.getStatus()) {
    		logger.error("The server encountered an unexpected condition.");
    	}//else if
    	else if(status == HttpStatusCode.SERVICEUNAVAILABLE.getStatus()) {
    		logger.error("The server is currently unavailable, retry later.");
    	}//else if
    	else {
    		logger.error("An unknown HTTP-Response-Code was returned.");
    	}//else
    }
    
    /**
     * cuts the input-string after 60 characters
     * All OMF JSON identities should be less than 255 characters in length. As a rule of thumb, keep the sizes of all names and identifies to 50 – 60 characters 
     * at most (although even these sizes might result in your application being difficult to read and understand.) 
     * @param input
     * @return cutted string
     */
    public String cutString(String input) {
    	return input.substring(0, 60);
    }
    
    /**
     * checks the value if it's a special floating number like NaN or infinity
     * @param value
     * @return
     */
    public boolean isSpecialFloatingPointValue(TypedValue<?> value){
    	
    	if(value.getType() == DataType.DOUBLE) {
    		logger.info("Value (DOUBLE) is not a real number and won't be published...");
    		return ((Double)value.getValue()).isNaN() || ((Double)value.getValue()).isInfinite();
    	}
    	
    	if(value.getType() == DataType.FLOAT) {
    		logger.info("Value (FLOAT) is not a real number and won't be published...");
    		return ((Float)value.getValue()).isNaN() || ((Float)value.getValue()).isInfinite();
    	}

    	return false;	
    }
}


