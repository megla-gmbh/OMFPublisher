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

/**
 * OMFContainerMessage.java
 *
 * OMFContainerMessage creates the container when a new Asset or Channel was created. 
 * It specifies the container definition in JSON-format.
 */
public class OMFContainerMessage {
	/**
	 * <b>id</b> 			Unique identifier of the Container.
	 * <b>typeid</b> 		ID of the Type used by the Container.
	 * <b>typeVersion</b> 	(optional) version of the type. If omitted version 1.0.0.0.
	 * <b>name</b> 			(optional) friendly name for the Container.		
	 * <b>description</b> 	(optional) description for the Container.
	 * <b>tags</b> 			(optional) array of strings to tag the Container.
	 * <b>metadata</b> 		(optional) key-value pairs associated with the Container.
	 * <b>indexes</b> 		(optional) array of Type Property ids to be used as secondary indexes for the Container.
	 */
	private String id;
	private String typeid;
	private String typeVersion;
	private String name;
	private String description;
	private String tags[];
	private String metadata;
	private String indexes[];
	
	/**
	 * Constructor which sets id, typeid, typeversion, name, description, tags, metadata and indexes of a Container message.
	 */
	public OMFContainerMessage(String id, String typeid, String typeVersion, String name, String description, String tags[], String metadata, String indexes[]) {
		this.id = id;
		this.typeid = typeid;
		this.typeVersion = typeVersion;
		this.name = name; 
		this.description = description;
		this.tags = tags;
		this.metadata = metadata;
		this.indexes = indexes;
	}
	
	/**
	 * Constructor of a simple container message, which sets the id and typeid.
	 */
	public OMFContainerMessage(String id, String typeid) {
		this.id = id;
		this.typeid = typeid;
	}
}
