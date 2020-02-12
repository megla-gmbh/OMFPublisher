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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;

import com.google.gson.Gson;

import de.megla.iot.OMFPublisher.OMFPublisherOptions;

/**
 * OMFChannel.java
 *
 * A channel serves to provide further information about an asset. 
 * Each asset has multiple channels for reading and writing data to / from the IoT device. 
 * It contains the associated IDs of the types and containers and the data
 */
public class OMFChannel {
	/**
	 * <b>channelname</b> 			the name of the channel that has been assigned in Kura-Wire
	 * <b>omfAsset</b> 				the Asset that belongs to this channel
	 * <b>typedValue</b> 			contains the value and its type of the channel
	 * <b>timestamp</b> 			current timestamp when the channel is created 
	 * <b>omfPublisherOptions</b> 	contains option-data for example the producertoken, hostname, targeturl
	 * <b>dateFormatter</b> 		Formatter for the timestamp
	 * <b>MAX_ARRAY_ELEMENTS</b>	constant to define the max amount of elements which an array could contain
	 */
	
	private String channelname;
	private OMFAsset omfAsset;
	private TypedValue<?> typedValue;
	private Date timestamp;
	private OMFPublisherOptions omfPublisherOptions;
	private SimpleDateFormat dateFormatter;  

	private static final int MAX_ARRAY_ELEMENTS = 3;
	
	/**
	 * Constructor which sets the name of the channel, options and the associated asset. 
	 * In addition, we set the format of the date.
	 */
	public OMFChannel(String name, OMFPublisherOptions omfPublisherOptions, OMFAsset myAsset){
		this.channelname=name;
		this.omfAsset=myAsset;
		this.omfPublisherOptions=omfPublisherOptions;
		
		dateFormatter= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Calendar cal  = Calendar.getInstance();
		this.timestamp = cal.getTime();
	}

	/**
	 * Returns the ID of the container of this channel
	 * @return unique string consisting of host name, asset name, channel name and channel type. Each separated by a "_".
	 */
	public String getContainerID(){
		return this.omfPublisherOptions.getDevicename()+"_"+this.omfAsset.getAssetname()+"_"+this.getChannelname()+"_"+ this.getTypedValue().getType().name();
	}
	
	/**
	 * Returns a JSON formatted string for the type definition appended to the asset.
	 */
	public String getTypeMessageJSON(){
		Gson gson = new Gson();
		StringBuilder sb = new StringBuilder();
		Map<String, Property> properties = new HashMap<>();
		Property propChannel;
		
		if(typedValue.getType()==DataType.BYTE_ARRAY) {
			//if the Array-Type is used there must be an "items" key which defines the type for each array-element
			//NOTE: MAX_ARRAY_ELEMENTS in real is the exact number of elements which has to be in the array!
			//in this case "integer"
			String ArrayElementType = "integer";
			propChannel = new Property(getOMFType(), new Property(ArrayElementType), MAX_ARRAY_ELEMENTS);
		} else {
			propChannel = new Property(getOMFType());
		}
		//create a new Channelname for the destination system to switch between Datatypes in Wire and
		//Build your Key-Values of properties up here and put it into the Map
		sb.append(channelname).append("_").append(this.getTypedValue().getType().name());
			
		Property propDate = new Property("string", "date-time", true);
		properties.put(sb.toString(), propChannel);
		properties.put("IndexedDateTime", propDate);
		
		return gson.toJson(new OMFTypeMessage(getTypeID(), "object", "dynamic", properties));
	}
	
	/**
	 * Returns a JSON formatted string representing the container for this channel.
	 */
	public String getContainerMessageJSON(){
		Gson gson = new Gson();
		return gson.toJson(new OMFContainerMessage(getContainerID(), getTypeID()));
	}
	
	/**
	 * Returns a string formatted in JSON for the data. This data is displayed in the destination system.
	 */
	public String getDataMessageJSON(){
		Gson gson = new Gson();
		StringBuilder sb = new StringBuilder();
		
		//create a new Channelname for the destination system to switch between Datatypes in Wire and create the data which will be send
		//to the destination system.
		sb.append(channelname).append("_").append(this.getTypedValue().getType().name());
		ArrayList<HashMap<String, Object>> values = new ArrayList<>();
		HashMap<String, Object> elements = new HashMap<>();
		
		elements.put("IndexedDateTime", this.dateFormatter.format(this.getTimestamp()));
		elements.put(sb.toString(), getValueWithDataType());
		values.add(elements);

		return gson.toJson(new OMFDataMessage(this.getContainerID(), values));	
	} 
	
	/**
	 * Returns the actual channelname.
	 */
	public String getChannelname() {
		return channelname;
	}
	
	/**
	 * Returns the type and value of the channel as an Object.
	 */
	public TypedValue<?> getTypedValue() {
		return typedValue;
	}

	/**
	 * Returns the current time stamp.
	 */
	public Date getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Returns the Type ID of this channel.
	 * @return unique string consisting of host name, asset name, channel name and channel type. Each separated by a "_".
	 */
	private String getTypeID(){
		return this.omfPublisherOptions.getDevicename()+"_"+this.omfAsset.getAssetname()+"_"+this.getChannelname()+"_"+ this.getTypedValue().getType().name();
	}
	
	/**
	 * Depending on the type of value of the channel, the destination-system-compliant type is returned
	 */
	private String getOMFType(){
		if(typedValue.getType()==DataType.BOOLEAN){
			return "integer";
		}else if(typedValue.getType()==DataType.INTEGER){
			return "integer";
		}else if(typedValue.getType()==DataType.DOUBLE){
			return "number";
		}else if(typedValue.getType()==DataType.FLOAT){
			return "number";
		}else if(typedValue.getType()==DataType.STRING){
			return "string";
		}else if(typedValue.getType()==DataType.LONG){
			return "integer";
		}else if(typedValue.getType()==DataType.BYTE_ARRAY){
			return "array";
		}else {		
			return "string";
		} //else
	}

	/**
	 * Gets the value for creating the JSON-String for sending the data.
	 */
	private <Any> Any getValueWithDataType(){
		if(typedValue.getType()==DataType.BOOLEAN) {
			if((Boolean)typedValue.getValue()){
				return (Any) (Integer) 1;
			}else{
				return (Any) (Integer) 0;
			} //else
		}else if(typedValue.getType()==DataType.INTEGER){
			return (Any)(Integer)typedValue.getValue();
		}else if(typedValue.getType()==DataType.DOUBLE){
			return (Any)(Double)typedValue.getValue();
		}else if(typedValue.getType()==DataType.FLOAT){
			return (Any)(Float)typedValue.getValue();
		}else if(typedValue.getType()==DataType.STRING){
			return (Any)("\""+typedValue.getValue().toString()+"\"");
		}else if(typedValue.getType()==DataType.LONG){
			return (Any)(Long)typedValue.getValue();
		}else if(typedValue.getType()==DataType.BYTE_ARRAY){
			return (Any)(Byte[])typedValue.getValue();
		} else {
			return (Any) ("\""+typedValue.getValue().toString()+"\"");
		}
	}
	
	/**
	 * Sets the current timestamp.
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * Sets the current type and value of the channel.
	 */
	public void setTypedValue(TypedValue<?> typedValue) {
		this.typedValue = typedValue;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(String.format("Channel name: %s", this.channelname)).append(System.lineSeparator());
		builder.append(String.format("timestamp: %s", this.timestamp)).append(System.lineSeparator());
		builder.append(String.format("datatype: %s", this.typedValue.getType().toString())).append(System.lineSeparator());
		builder.append(String.format("value: %s", this.typedValue.getValue().toString())).append(System.lineSeparator());
		
		return builder.toString();
	}
}
