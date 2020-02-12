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

import java.util.HashMap;
import java.util.Map;

/**
 * OMFTypeMessage.java
 *
 * OMFTypeMessage creates the type when a new Asset or Channel was created or modified. 
 * It specifies the type definition in JSON-format.
 */
public class OMFTypeMessage {
	/**
	 * <b>id</b> 				identification name of the type definition
	 * <b>classification</b> 	classification must be "static" or "dynamic"
	 * <b>type</b> 				type must be set to "object"
	 * <b>properties</b>  		properties of the type definition. With at least two elements in the first object (isindex: boolean, type= "string")
	 * <b>description</b> 		(optional) description of the type definition
	 * <b>version</b> 			(optional) version of the type. If omitted version 1.0.0.0.
	 */
	private String id;
	private String description;
	private String type;
	private String classification;
	private Map<String, Property> properties = new HashMap<>(); 
	private String version;
	
	/**
	 * Constructor which sets id, description, type, classification, properties and version.
	 */
	public OMFTypeMessage(String id, String description, String type, String classification, Map<String, Property> prop, String version) {
		this.id = id;
		this.classification = classification;
		this.type = type;
		this.description = description;
		this.properties = prop;
		this.version = version;
	}
	
	/**
	 * Constructor which sets id, type, classification and properties.
	 */
	public OMFTypeMessage(String id, String type, String classification, Map<String, Property> prop) {
		this.id = id;
		this.classification = classification;
		this.type = type;
		this.properties = prop;		
	}
}
