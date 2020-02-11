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

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.kura.internal.wire.asset.WireAssetConstants;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OMFValidator.java
 * 
 * @author Niklas Rose, Till Böcher
 */
public class OMFValidator {
	private static final int OMF_MAX_MESSAGE_SIZE_KB = 192;
	private static final Logger logger =  LoggerFactory.getLogger(OMFValidator.class);
	
    //IMPORTANT: unsupportet Types will not be handled
    //OMF target does not support  these types.
    private static List<DataType> forbiddenTypes = Arrays.asList(DataType.BYTE_ARRAY);
    private static List<String> ignoredWireRecordProperties = Arrays.asList(WireAssetConstants.PROP_ASSET_NAME.value(),
    																					WireAssetConstants.PROP_SINGLE_TIMESTAMP_NAME.value());
    
	/**
     * Checks a for NaN and infinity values.
     * NaN and infinity are not supported by OMF.
     */
    public static boolean isSpecialFloatingPointValue(TypedValue<?> value){
      
      if(value.getType() == DataType.DOUBLE) {
             boolean result = ((Double)value.getValue()).isNaN() || ((Double)value.getValue()).isInfinite();
             if (result) {
                    logger.info("Value (DOUBLE) is not a real number and won't be published...");
             }                   
             
             return result;
      }
      
      if(value.getType() == DataType.FLOAT) {
             boolean result = ((Float)value.getValue()).isNaN() || ((Float)value.getValue()).isInfinite();
             if (result) {
                    logger.info("Value (FLOAT) is not a real number and won't be published...");
             }
             
             return result;
      }

      return false;      
    }
    
    /**
     * Check if the a HTTP status code is ok.
     */
    public static boolean isPositiveOmfHttpResponse(int httpResponseCode) {
    	return httpResponseCode == HttpStatusCode.NOCONTENT.getStatus() || 
    		httpResponseCode == HttpStatusCode.ACCEPTED.getStatus() || 
    		httpResponseCode == HttpStatusCode.OK.getStatus() || 
    	    httpResponseCode == HttpStatusCode.BADREQUEST.getStatus();
    }
    
    /**
     * True if properties are complete.
     */
    public static boolean checkProperties(OMFPublisherOptions omfPublisherOptions) {
    	boolean result = true;
		
    	if(omfPublisherOptions.getProducerToken().trim().isEmpty()) {
    		result = false;
    		logger.error("OMFPublisherOptions: ProducerToken empty."); 
		}
		
		if(omfPublisherOptions.getTargetURL().trim().isEmpty()) {
			result = false;
    		logger.error("OMFPublisherOptions: TargetUrl empty."); 
		}

		if(omfPublisherOptions.getDevicename().trim().isEmpty()) {
			result = false;
    		logger.error("OMFPublisherOptions: Devicename empty."); 
		}
		
		return result;
	}
    
    /**
     * Checks if the OMF message exceeds the max OMF message size (192kB).
     */
	public static boolean isLargerThanOmfMessageMaxSize(String message) {
		return message.getBytes().length > (OMF_MAX_MESSAGE_SIZE_KB*1024);
	}
	
	/**
	 * Checks if the wire recored contains a value 
	 * with a type that is not supported by OMF.
	 */
	public static boolean checkWireRecordForForbiddenType(WireRecord record) {
		boolean result = false;
		
		for(Entry<String, TypedValue<?>> entry: record.getProperties().entrySet()) {
			if(!ignoredWireRecordProperties.contains(entry.getKey()) &&
					forbiddenTypes.contains(entry.getValue().getType()))
				result = true;
		}
		
		return result;
	}
}
