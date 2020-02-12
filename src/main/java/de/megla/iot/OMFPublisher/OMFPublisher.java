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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.ssl.SslManagerService;
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
import de.megla.iot.OMFPublisher.models.OMFAssetList;
import de.megla.iot.OMFPublisher.models.OMFAssetListQueue;
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
	 * <b>omfPublisherService</b> 		provides the ability to write individual definitions of Type, Container and Data 
	 * 									in a suitable JSON format. In addition, it creates the header according to the "messagetype" 
	 * 									and makes a request to the destination system
	 * <b>SslManagerService</b>			Service for settings the SSL connections settings.
	 * <b>wireHelperService</b> 		Interface is used as a utility API to provide fast and necessary operations for the Kura Wires.
	 * <b>wireSupport</b>				Interface for managing incoming and outgoing wire components. 
	 * 									Additionally for use, e.g. To send and receive wireRecords.
	 * <b>knownAssetList</b>			Key-Value store of the last known Assets and their Channels
	 * <b>properties</b>				properties which contain service configurations and user specific inputs
	 * <b>inFlightMessages</b>			List of all unpublished Messages as an in-memory datacaching
	 * <b>executorServiceInFlight</b> 	Scheduled Thread, which sends all cached Messages to a connector
	 * <b>configurationComplete</b>		True when all properties (target url, producer token, device name) are set.
	 */
    private static final Logger logger =  LoggerFactory.getLogger(OMFPublisher.class);
    
    private OMFPublisherOptions omfPublisherOptions;

	private OMFPublisherService omfPublisherService;
    
    private SslManagerService sslManagerService;
    
    private WireHelperService wireHelperService;
    private WireSupport wireSupport;
    
    private OMFAssetList knownAssetList;
    private Map<String, Object> properties;
    
    private OMFAssetListQueue inFlightMessages = new OMFAssetListQueue();
    private ScheduledExecutorService executorServiceInFlight = Executors.newScheduledThreadPool(1);    

    private boolean configurationComplete = false;
    
    // ----------------------------------------------------------------
    // Dependencies
    // ----------------------------------------------------------------

    /**
     * Binds the Wire Helper Service.
     */
    @Reference(unbind="unbindWireHelperService")
    public void bindWireHelperService(final WireHelperService wireHelperService) {
        
    	if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
            logger.debug("Wire Helper service bound.");
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     */
    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        
    	if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
            logger.debug("Wire Helper service unbound.");
        }
    }
    
    /**
     * Binds the SSL manager service.
     */
    @Reference(unbind="unbindSslManagerService")
    public void bindSslManagerService(final SslManagerService sslManagerService) {
    	
    	if (isNull(this.sslManagerService)) {
            this.sslManagerService = sslManagerService;
            logger.debug("SSL manager service bound.");
        }
    }
    
    /**
     * Unbinds the SSL manager service.
     */
    public void unbindSslManagerService(final SslManagerService sslManagerService) {
    	
    	if(this.sslManagerService == sslManagerService) {
    		this.sslManagerService = null;
    		logger.debug("SSL manager service unbound.");
    	}
    }

    // ----------------------------------------------------------------
    // Activation APIs
    // ----------------------------------------------------------------

    /**
     * OSGi Service Component callback for activation.   
     */
    @Activate
    protected void activate( final Map<String, Object> properties) {   
    	try {
	        logger.info("Activating OMF Publisher Wire Component...");
	        this.wireSupport = this.wireHelperService.newWireSupport(this); 
	        
	        updated(properties);
	        startInFlightMessagesTask();

	        logger.info("Activating OMF Publisher Wire Component... Done");
    	} catch(Exception e) {	
			ErrorHandling.handle("Error in activating OMF Publisher Wire Component", e, logger);
 		}
    }

    /**
     * OSGi Service Component callback for updating.
     */
    @Modified
    public void updated(final Map<String, Object> properties) {
        logger.info("Updating OMF Publisher Wire Component...");
        
        this.properties = properties;
        this.omfPublisherOptions = new OMFPublisherOptions(this.properties);
        this.knownAssetList = new OMFAssetList(this.omfPublisherOptions);
        
        if(OMFValidator.checkProperties(this.omfPublisherOptions)) {
        	this.configurationComplete = true;
        	resetPublisherOptions();
		    
		    logger.info("Updating OMF Publisher Wire Component... Done");
        }
        else {
        	this.configurationComplete = false;
        	shutdownMessageService();
        }
    }

    /**
     * Resets the in flight message service. 
     */
	private void resetPublisherOptions() {
		shutdownMessageService();
		
		// Start Thread for sending in flight messages
		startInFlightMessagesTask();     
		
		//Remove all known assets in order to send definitions again.
		this.knownAssetList.clear();
		
		//Update last to set new OMFPublisher and SSL Options 
		this.omfPublisherService = new OMFPublisherService(this.omfPublisherOptions, this.sslManagerService);
	}

	/**
	 * Shutsdown the message service an creates a new thread pool.
	 */
	private void shutdownMessageService() {
		if(!this.executorServiceInFlight.isShutdown()) {
			this.executorServiceInFlight.shutdown();
	    	executorServiceInFlight = Executors.newScheduledThreadPool(1);
			logger.info("Shutdown message service.");
		}
	}

	/**
     * OSGi Service Component callback for deactivation.
     */
    @Deactivate
    protected void deactivate() {
        logger.info("Deactivating OMF Publisher Wire Component...");
        
        if(executorServiceInFlight != null) {
        	executorServiceInFlight.shutdown();
        }
        
        logger.info("Deactivating OMF Publisher Wire Component... Done");
    }

    /** 
	 * Listener is called, when an WireEnvelope is received.
     */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");

        if (nonNull(this.omfPublisherOptions)) {
            final List<WireRecord> records = wireEnvelope.getRecords();
            publish(records);
        }
    }

    /**
     * Inherited method from interface org.osgi.service.wireadmin.Consumer.
     */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /**
     * Inherited method from interface org.osgi.service.wireadmin.Consumer.
     */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    /**
     * Publishes the list of provided {@link WireRecord}s.
     */
    private void publish(final List<WireRecord> records) {
    	logger.info(String.format("Adding asset and data to in flight message queue... (%d remaining)", this.inFlightMessages.size()));
    	
    	if(!this.configurationComplete)
    		logger.error("Publisher configuration incomplete. Message will not be published.");
    	
        requireNonNull(records, "Wire Records cannot be null");  
        
        OMFAssetList currentAssetList = createOMFAssetListFromWireRecords(records);
        
        if(currentAssetList.isEmpty())
        	return;
        
        try {
			synchronized(this.inFlightMessages) {
				this.inFlightMessages.add(currentAssetList);
			}				
        } catch(NullPointerException e) {
        	ErrorHandling.handle("Error in publishing wire records using PIOMF publisher..", e, logger);
        }
    }
    
	/**
     * Method creates a full List of all Assets and their Channels from WireRecords.
     */
    private OMFAssetList createOMFAssetListFromWireRecords(final List<WireRecord> wireRecords){
    	
    	OMFAssetList assetList = new OMFAssetList(this.omfPublisherOptions);
    	
	    for (final WireRecord dataRecord : wireRecords)
	    	assetList.addAssetFromWireRecord(dataRecord);
	    
    	return assetList;
    }

    /**
     * Checks if an Asset or Channel was modified in Wire and put it into the knownAssetList.
     */
    private boolean isAssetOrChannelModified(OMFAssetList currentAssetList) {
        //Check if types and containers are all included as they have been sent before
        boolean isModified =false;
        
        //Check asset and if it is not already in the map set the flag
        for (OMFAsset asset : currentAssetList) {
	       	String assetname=asset.getAssetname();
	       	assetname = StringExtensions.convertToOmfString(assetname);
	     
	       	if(!this.knownAssetList.containsAsset(assetname) || this.knownAssetList.isEmpty()){
	       		
	       		isModified=true;
	       		logger.debug("New Asset "+assetname+ " detected");
		 		this.knownAssetList.add(new OMFAsset(assetname,this.omfPublisherOptions));
			}
	       	
			//Check channel and add it to the map, if it is not already in the map set the flag
			for (Map.Entry<String, OMFChannel> channelEntry : asset.getChannels().entrySet()){
				
			 	String channelname=channelEntry.getKey();
			 	channelname = StringExtensions.convertToOmfString(channelname);
			 	OMFChannel channel = channelEntry.getValue();
			 	
			 	if(!this.knownAssetList.getAsset(assetname).getChannels().containsKey(channelname) || 
			 		
			 		this.knownAssetList.getAsset(assetname).getChannels().get(channelname).getTypedValue().getType() != channel.getTypedValue().getType()) {
			 		isModified=true;
			 		
			 		logger.debug("New Channel "+channelname+" detected");
			 		
			 		OMFChannel newChannel=new OMFChannel(channelname,this.omfPublisherOptions, asset);	
			 		newChannel.setTypedValue(TypedValues.newTypedValue(channel.getTypedValue().getValue()));
			 		this.knownAssetList.getAsset(assetname).getChannels().put(channelname, newChannel);
			 	}
		    }	 
        }
        
        return isModified;
    }
    
    /**
     * Sends the message, of a modified Asset or Channel to the destination system with a new type-, container and data definition.
     */
    private void sendModifiedMessage() {
    	
    	boolean typeSent = sendTypeMessage(this.knownAssetList);
		
		boolean containerSent = sendContainerMessage(this.knownAssetList);
			
		if(typeSent && containerSent) 
			sendDataMessageLinks(this.knownAssetList);
    }
    
    /**
     * Sends the first in-flight message to the destination system. If there is something wrong with the connection or message, 
     * the message parameters are logged. In the end remove the first list entry of in-flight messages.
     */
    private synchronized void sendInFlightMessage() {
        logger.info(String.format("Trying to send next In-flight Message. (%d remaining)", this.inFlightMessages.size()));
        
        int status;
        
        OMFAssetList inFlightAssetList = this.inFlightMessages.get(0);
        
        try {
            
            if(isAssetOrChannelModified(inFlightAssetList)) {
                this.sendModifiedMessage();
            }
            
            status = sendDataMessage(inFlightAssetList);
            
            if(OMFValidator.isPositiveOmfHttpResponse(status))                 
                logger.info(String.format("Sent in-flight <Data> correctly...%s", System.lineSeparator()));
            else
            	ErrorHandling.handle(logger
            			, "Connection to OMF target can be established, but the OMF target did not accept the message."
            			, "Data will be discarded."
            			, inFlightAssetList.toString());
            
            synchronized(this.inFlightMessages) {
                this.inFlightMessages.remove();
            }            
    
        }catch(Exception ex) {
            ErrorHandling.handle(" Error during sending In Flight Messages: ", ex, logger);
        }
    } 

    /**
     * Sends the Type Message for all known assets.
     * @return True if status is accepted as ok.
     */
    private boolean sendTypeMessage(OMFAssetList assets) {
    	boolean result = false;
    	
    	//Generate the type from all assets and channels
    	String typeDefinitionJSON= this.omfPublisherService.createTypeMessage(assets); 
	    int status = this.omfPublisherService.handleOMFMessageRequest("create", "type", typeDefinitionJSON);
	    
	    if(!OMFValidator.isPositiveOmfHttpResponse(status))
			ErrorHandling.httpStatusToErrorLog(status, logger);
	    
		else {
			result = true;
			logger.info("Sent <Type> correctly..." );
		}
	    return result;
    }
    
    /**
     * Sends the Container Message for all known assets.
     * @return True if status is accepted as ok.
     */
    private boolean sendContainerMessage(OMFAssetList assets) {
    	boolean result = false;
    	
    	//Generate containers from all assets and channels
	    String containerDefinitionJSON= this.omfPublisherService.createContainerMessage(assets);
	    int status = this.omfPublisherService.handleOMFMessageRequest("create", "container", containerDefinitionJSON);
	    
	    if(!OMFValidator.isPositiveOmfHttpResponse(status))
	    	ErrorHandling.httpStatusToErrorLog(status, logger);
	    
		else {
			result = true;
			logger.info("Sent <Container> correctly...");
		}
	    
	    return result;
    }
    
    /**
     * Sends the Data Message for all known assets and their links.
     */
    private void sendDataMessageLinks(OMFAssetList assets) {

    	//Create assets and links	  
		String assetandlinksJSON = this.omfPublisherService.createAssetsAndLinks(assets);
		int status = this.omfPublisherService.handleOMFMessageRequest("create", "data", assetandlinksJSON);	
	    
		if(!OMFValidator.isPositiveOmfHttpResponse(status))
			ErrorHandling.httpStatusToErrorLog(status, logger);		
		
		else {
			logger.info(String.format("Sent <Data Links> correctly...%s", System.lineSeparator()));	
		}
    }
    
    /**
     * Sends the Data Message for all known assets.
     */
    private int sendDataMessage(OMFAssetList assets) {
    	
    	// If ready, send finished JSON with the containers and payload
    	// only from the currently received wires, hence currentAssetList
    	String dataJSON= this.omfPublisherService.createDataValuesMessage(assets);
    	
    	int status = -1;
    	
    	// Send the JSON message to the target URL
    	status = this.omfPublisherService.handleOMFMessageRequest("create", "data", dataJSON);
    	
    	return status;
    }
    
    /**
     * Starts a Thread that checks if there are saved in-flight messages. When the publisher is connected with the destination system
     * again, send every in-flight message.
     */
    private void startInFlightMessagesTask() {    	
    	final int inFlightInterval = this.omfPublisherOptions.getinFlightInterval();
    	
    	logger.info("Start Task to sending in-flight messages...");
    	
    	//Runnable Task to send in-flight messages if possible  
    	try {
    		Runnable runnableTaskSendMessage = () -> {
				if(!inFlightMessages.isEmpty() && OMFValidator.isPositiveOmfHttpResponse(isConnectionToOMFTargetEstablished())) {
				    //send every in-flight message one by one every x seconds
					sendInFlightMessage();
				}
    		};
		
    		executorServiceInFlight.scheduleWithFixedDelay(runnableTaskSendMessage, 100, inFlightInterval, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			ErrorHandling.handle("In-flight message delivery failed.", e, logger);
		}
    }

	/**
	 * Send HTTP request to the  Connector to check if a connection can be established.
	 * @return HTTP status code of the response
	 */
    private int isConnectionToOMFTargetEstablished() {
    	logger.debug("Checking the connection to the OMF target.");
    	int result = 404; 
		
		HttpsURLConnection response = this.omfPublisherService.sendOMFMessage("create", "type", "[]");
		if(response != null) {
			try {
				result = response.getResponseCode();
			} catch (IOException ex) {
				ErrorHandling.handle("Connection to the omf target could not be ", ex, logger);
			}
		}
		
		logger.debug(String.format("Connection check http code result: %d", result));
		return result;
	 }
}


