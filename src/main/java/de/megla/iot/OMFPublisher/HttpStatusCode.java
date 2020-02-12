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
 * HttpStatusCode.java
 * 
 * Enum which defines all necessary status codes for an HTTP-request
 */
public enum HttpStatusCode {
	/**
	 * <b>code</b> 			number of a status code 
	 * <b>description</b>	description of the status code
	 */
	OK(200, "OK"),
	ACCEPTED(202, "Accepted"),
	NOCONTENT(204, "No Content"),
	BADREQUEST(400, "Bad Request"), 
	UNAUTHORIZED(401, "Unauthorized"), 
	FORBIDDEN(403, "Forbidden"), 
	PAYLOADTOOLARGE(413, "Payload Too Large"), 
	INTERNALSERVERERROR(500, "Internal Server Error"), 
	SERVICEUNAVAILABLE(503, "Service Unavailable"), 
	UNKNOWN(-1, "Unknown");
	
	private int code;
	private String description;

	/**
	 * Constructor of an HTTP status Code.
	 */
    HttpStatusCode(int code, String desc) {
        this.code = code;
        this.description = desc;
    }
    
    /**
     * Gets the HTTP status code as an Integer.
     */
    public int getStatus() { 
        return this.code;
    }
    
    /**
     * Gets the HTTP status code description.
     */
    public String getDescription() {
    	return this.description;
    } 
}


