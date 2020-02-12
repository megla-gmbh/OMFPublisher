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
package de.megla.iot.OMFPublisher.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.eclipse.kura.internal.wire.asset.WireAssetConstants;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.megla.iot.OMFPublisher.OMFPublisherOptions;
import de.megla.iot.OMFPublisher.OMFValidator;
import de.megla.iot.OMFPublisher.StringExtensions;

/**
 * OMFAssetList.java
 */
public class OMFAssetList extends ArrayList<OMFAsset> {
	private static final Logger logger =  LoggerFactory.getLogger(OMFValidator.class);
	
    //Keys to get information from the wireRecords
    private static final String ASSET_NAME_PROPERTY_KEY = WireAssetConstants.PROP_ASSET_NAME.value().toString();
    private static final String SINGLE_TIMESTAMP_NAME = WireAssetConstants.PROP_SINGLE_TIMESTAMP_NAME.value().toString();
    private static final String SUFFIX_TIMESTAMP = WireAssetConstants.PROP_SUFFIX_TIMESTAMP.value().toString();
    
    private OMFPublisherOptions omfPublisherOptions;
    
    /**
     * Constructor
     */
    public OMFAssetList(OMFPublisherOptions omfPublisherOptions) {
    	this.omfPublisherOptions = omfPublisherOptions;
    }    
    
    /**
     * Adds asset form a WireRecord.
     */
	public void addAssetFromWireRecord(WireRecord wireRecord) {
		Map<String, TypedValue<?>> wireRecordProps = wireRecord.getProperties();
		
		//Name is necessary for OMF target
		if(!wireRecordProps.containsKey(ASSET_NAME_PROPERTY_KEY))
			throw new NullPointerException("Wirerecord does not contain assetname information");
		
		if(OMFValidator.checkWireRecordForForbiddenType(wireRecord)) {
    		logger.info("A channel with the datatype byte array is configured. Byte array is not supported.");
    		return;
		}
		
    	String assetname = (String)wireRecord.getProperties().get(ASSET_NAME_PROPERTY_KEY).getValue();
    	assetname = StringExtensions.convertToOmfString(assetname);
	
    	if(!this.containsAsset(assetname))
 			this.add(new OMFAsset(assetname,this.omfPublisherOptions));
    	
    	OMFAsset newAsset = this.getAsset(assetname);
    	
		//single timestamp or channel timestamp
		if(wireRecordProps.containsKey(SINGLE_TIMESTAMP_NAME))
			for (Map.Entry<String, TypedValue<?>> entry : wireRecordProps.entrySet())
		        extractAssetWithSingleTimestamp(wireRecord, newAsset, entry);
		else			
			for (Map.Entry<String, TypedValue<?>> entry : wireRecordProps.entrySet())
				extractAssetWithChannelTimestamp(wireRecord, newAsset, entry);
		
	}
	
	/**
	 * Checks if the list contains a asset with the specified name.
	 */
	public boolean containsAsset(String assetName) {
		boolean result = false;
		
		result = this.stream().filter(x -> x.getAssetname().equals(assetName)).count() > 0;
		
		return result;		
	}
	
	/**
	 * Return a asset with the specified name.
	 */
	public OMFAsset getAsset(String assetName) {
		return this.stream().filter(x -> x.getAssetname().equals(assetName)).findFirst().get();
	}
	
	/**
	 * Extracts a asset with a channel time stamp from a WireRecord into a OMFAsset.
	 */
	private void extractAssetWithChannelTimestamp(final WireRecord dataRecord, OMFAsset asset,
			Map.Entry<String, TypedValue<?>> entry) {
		String key=entry.getKey();
		
		if(key.contains(SUFFIX_TIMESTAMP)){
			
			// if there is value which is not serializable with JSON jump over this entry
			if(OMFValidator.isSpecialFloatingPointValue(entry.getValue()))
				return;
			
			String channelname=key.replace(SUFFIX_TIMESTAMP, "");
			channelname = StringExtensions.convertToOmfString(channelname);
			
			//Add channel if it is not already created
			if(!asset.getChannels().containsKey(channelname)){
				OMFChannel newchannel=new OMFChannel(channelname,this.omfPublisherOptions, asset);
				asset.getChannels().put(channelname, newchannel);
			}
			
			OMFChannel channel=asset.getChannels().get(channelname);
			TypedValue<?> typedTimestamp= dataRecord.getProperties().get(key);
			
			channel.setTimestamp(new Date((Long)typedTimestamp.getValue()));	
			channel.setTypedValue(dataRecord.getProperties().get(channelname));	
		}
	}

	/**
	 * Extracts a asset with a asset(single) time stamp from a WireRecord into a OMFAsset.
	 */
	private void extractAssetWithSingleTimestamp(final WireRecord dataRecord, OMFAsset asset,
			Map.Entry<String, TypedValue<?>> entry) {
		String key=entry.getKey();
		
		if(!(key.equals(SINGLE_TIMESTAMP_NAME) || key.equals(ASSET_NAME_PROPERTY_KEY))){
			
			// if there is value which is not serializable with JSON jump over this entry
			if(OMFValidator.isSpecialFloatingPointValue(entry.getValue()))
				return;
			
			String channelname=key;
			channelname = StringExtensions.convertToOmfString(channelname);
			
			//Add channel if it is not already created
			if(!asset.getChannels().containsKey(channelname)){
				OMFChannel newchannel=new OMFChannel(channelname,this.omfPublisherOptions, asset);
				asset.getChannels().put(channelname, newchannel);
			}
			
			OMFChannel channel=asset.getChannels().get(channelname);
			TypedValue<?> typedTimestamp = dataRecord.getProperties().get(SINGLE_TIMESTAMP_NAME);
			
			channel.setTimestamp(new Date((Long)typedTimestamp.getValue()));
			channel.setTypedValue(dataRecord.getProperties().get(key));	
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		int assetCount = 0;
		
		for(OMFAsset asset: this) {
			assetCount++;
			builder.append(String.format("Asset %d:", assetCount)).append(System.lineSeparator());
			builder.append(asset.toString());
		}
		return builder.toString();
		
	}
    
}
