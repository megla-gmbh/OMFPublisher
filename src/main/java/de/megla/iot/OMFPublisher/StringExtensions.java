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

/**
 * StringExtensions.java
 * 
 * @author Niklas Rose, Till Böcher
 */
public class StringExtensions {
	public static int MAX_OMF_STRING_LENGTH = 60;
	
    /**
     * Removes all not allowed special characters from Asset- and Channelname and cuts the String if more than 60 Characters
     */
    public static String convertToOmfString(String input) {		 
    	String result = input.replaceAll("[\\[\\]\\|!?\\\\;`´{}()'*]", ""); 
    	if (result.length() >= MAX_OMF_STRING_LENGTH) {
    		result = cutToMaxOMFLenght(result);
    	} //if
    	return result;
    }
    
    /**
     * All OMF JSON identities should be less than 255 characters in length. (50 - 60 characters) 
     */
    public static String cutToMaxOMFLenght(String input) {
    	return input.substring(0, MAX_OMF_STRING_LENGTH);
    }
}
