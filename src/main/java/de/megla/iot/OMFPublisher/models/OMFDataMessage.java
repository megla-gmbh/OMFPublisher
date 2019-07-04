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

/**
 * OMFDataMessage.java
 *
 * @author Niklas Rose, Till Böcher 
 * Data messages are used to create static assets, link assets and container together and feed container values into destination end points
 * Data messages can span multiple Types and Containers. 
 * The body of a Data message is composed of an array of objects.
 * In an on-premises destination system, container values are presented as timeseries event values sent to data end points.
 */
public class OMFDataMessage {

	/**
	 * <b>typeid</b> 		(optional) ID of the type. If omitted, container is expected.
	 * <b>containerid</b> 	(optional) ID of the container. If omitted, type is expected.
	 * <b>typeVersion</b> 	(optional) version of the type. If omitted version 1.0.0.0.
	 * <b>values</b> 		An array (in OMF) of objects conforming to the type.
	 */
	private String typeid;
	private String containerid;
	private String typeVersion;
	private ArrayList<HashMap<String, Object>> values;
	
	/**
	 * Constructor which sets typeid, containerid, typeVersion and the array of values.
	 * @param typeid
	 * @param containerid
	 * @param typeVersion
	 * @param values
	 */
	public OMFDataMessage(String typeid, String containerid, String typeVersion, ArrayList<HashMap<String, Object>> values) {
		this.typeid = typeid;
		this.containerid = containerid;
		this.typeVersion = typeVersion;
		this.values = values;
	}
	
	/**
	 * Constructor which sets the containerid and the array of values.
	 * This constructor is usually used to create an object of an OMFDataMessage.
	 * @param typeid
	 * @param containerid
	 * @param typeVersion
	 * @param values
	 */
	public OMFDataMessage(String containerid, ArrayList<HashMap<String, Object>> values) {
		this.containerid = containerid;
		this.values = values;
	}
}
