/*******************************************************************************
 * Copyright (c) 2019 MEGLA GmbH and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MEGLA GmbH
 *******************************************************************************/

package de.megla.iot.OMFPublisher.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;

import de.megla.iot.OMFPublisher.OMFPublisherOptions;

/**
 * OMFAsset.java
 *
 * @author Niklas Rose, Till B�cher 
 * The class OMFAsset is used to create a new asset, to convert the type number 
 * using the assigned asset name and in JSON format and, accordingly, asset to 
 * attach the channels to the JSON object
 */
public class OMFAsset {
	/**
	 * <b>assetname</b> 			is the name of the asset that has been assigned in Kura-Wire
	 * <b>channels</b> 				saves all created channels with a key as channel name 
	 * 								and a value as an object of class OMFChannel
	 * <b>omfPublisherOptions</b> 	contains option-data for example the producertoken, hostname, targeturl
	 * <b>dataValues</b> 			list of "values" for the OMF-element "__Link"
	 */ 
	private String assetname;
	private Map<String, OMFChannel> channels =new HashMap<>();
	private OMFPublisherOptions omfPublisherOptions;
	private ArrayList<LinkedValues> dataValues = new ArrayList<>();
	
	/**
	 * Constructor which sets the name and options of the asset
	 * @param name User-assigned name of the created asset
	 * @param myPublisherOptions Options needed by the publisher
	 */
	public OMFAsset(String name,OMFPublisherOptions myPublisherOptions){
		this.assetname=name;
		this.omfPublisherOptions=myPublisherOptions;
	}
	
	// ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------  
	
	/**
	 * Returns the ID for the type of asset
	 * @return ID of the type and attached name of the asset
	 */
	private String getTypeID(){
		return this.omfPublisherOptions.getDevicename()+"_"+this.assetname;
	}
	
	// ----------------------------------------------------------------
    //
    // Public methods
    //
    // ----------------------------------------------------------------  
	
	/**
	 * returns the JSON object for the type definition
	 * @return String in JSON format of the message type
	 */
	public String getTypeMessageJSON(){
		Gson gson = new Gson();
		
		Map<String, Property> properties = new HashMap<>();
		
		Property propName = new Property("string", true);
		properties.put("Name", propName);
		
		return gson.toJson(new OMFTypeMessage(getTypeID(), "Kura IoT Asset", "object", "static", properties, null));
	}
		
	/**
	 * Returns the asset to be created and the JSON containers for the channels
	 * @return returns a JSON string containing asset data with corresponding channels
	 */
	public String getDataMessageJSON(){ 
		Gson gson = new Gson();
		String typeID = getTypeID();
		StringBuilder sb = new StringBuilder();
		String jsonLinkedDataMessage = "";
		String jsonAFElement = "";
		/*
		 * create Asset Element for each Asset
		 */
		ArrayList<LinkedValues> values = new ArrayList<>();
		ArrayList<HashMap<String, String>> properties = new ArrayList<>();
		HashMap<String, String> propertyValue = new HashMap<>();
		String propertyId = "Name";
		propertyValue.put(propertyId, typeID);	
		properties.add(propertyValue);
		AssetFrameworkElement afElement = new AssetFrameworkElement(typeID, properties);
		jsonAFElement = gson.toJson(afElement);
		/*
		 * Create Parent Asset Links
		 */
		String typeidSource = "KuraIoTDevice";
		String index = this.omfPublisherOptions.getDevicename();
		SourceTarget source = new SourceTarget(typeidSource, index);
		SourceTarget target = new SourceTarget(typeID, typeID);
		LinkedValues linkedValues = new LinkedValues(source, target);
		values.add(linkedValues);		
				
		//put parent Asset into list as first Element and add all Channels to it
		this.dataValues = values;
		putChannelsToValueList(typeID);
		
		LinkedDataMessage linkedDataMessage = new LinkedDataMessage("__Link", this.dataValues);
		jsonLinkedDataMessage = gson.toJson(linkedDataMessage);
		
		// Append the built AF-Element and Linked Data Message and seperate it with ","
		sb.append(jsonAFElement).append(",").append(jsonLinkedDataMessage);
		return sb.toString();
	}
	
	/**
	 * Adds all channels from the key value store to the JSON string
	 * @param typeID 
	 */
	public void putChannelsToValueList(String typeID) {						
		String typeid = typeID;
		String index = typeID;
		/*
		 * The parent Asset it the source for every Channel
		 */
		SourceTarget source = new SourceTarget(typeid, index);
		/*
		 * extract every Channels Source and Target data and add it to the linked value list
		 */
		for (Map.Entry<String, OMFChannel> channelEntry : channels.entrySet()){
			String containerID = channelEntry.getValue().getContainerID();
			SourceTarget target = new SourceTarget(containerID);
			LinkedValues sourceAndTarget = new LinkedValues(source, target);	
			this.dataValues.add(sourceAndTarget);
		} //for
	}
	
	// ----------------------------------------------------------------
    //
    // Getter methods
    //
    // ----------------------------------------------------------------  
	
	/**
	 * Gets the assetname.
	 * @return name of the asset
	 */
	public String getAssetname() {
		return assetname;
	}
	
	/**
	 * Gets all channels of this object. It returns a Map of OMFChannels of the actual OMFAsset.
	 * @return channels of the asset
	 */
	public Map<String, OMFChannel> getChannels() {
		return channels;
	}
}
