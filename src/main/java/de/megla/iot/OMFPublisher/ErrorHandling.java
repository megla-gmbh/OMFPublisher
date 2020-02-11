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

import org.slf4j.Logger;

/**
 * ErrorHandling.java
 *
 * @author Niklas Rose, Till Böcher 
 */
public class ErrorHandling {
	
	/**
	 * Logs the exceptions as an error.
	 */
	public static void handle(String message, Throwable ex, Logger logger) {
		logger.error(message, ex);
	}
	
	/**
	 * Logs multiple strings as errors.
	 */
	public static void handle(Logger logger, String...strings) {
		for(String line: strings)
			logger.error(line);
	}
	
	/**
	 * Creates an appropriate log message for different https status codes.
	 */
    public static void httpStatusToErrorLog(int status, Logger logger) {    	
    	if(status == HttpStatusCode.BADREQUEST.getStatus()) 
    		logger.error("The OMF message was malformed or not understood. The client should not retry sending the message without modifications.");
    	
    	if(status == HttpStatusCode.UNAUTHORIZED.getStatus()) 
    		logger.error("Authentication failed.");
    		
    	if(status == HttpStatusCode.FORBIDDEN.getStatus())
    		logger.error("Authentication succeeded, but not authorized.");
    	
    	if(status == HttpStatusCode.PAYLOADTOOLARGE.getStatus()) 
    		logger.error("Payload size exceeds OMF body size limit of 192kb.");
    	
    	if(status == HttpStatusCode.INTERNALSERVERERROR.getStatus()) 
    		logger.error("The server encountered an unexpected condition.");
    	
    	if(status == HttpStatusCode.SERVICEUNAVAILABLE.getStatus()) 
    		logger.error("The server is currently unavailable, retry later.");
    	
    	if(status == HttpStatusCode.UNKNOWN.getStatus())
    		logger.error("An unknown HTTP-Response-Code was returned.");
    }
}