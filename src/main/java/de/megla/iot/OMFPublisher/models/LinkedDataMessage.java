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

/**
 * LinkedDataMessage.java
 *
 * LinkedDataMessage is used to connect (link) assets and container together and create Asset Element/Attribute structure
 * Three types of link objects are important for creating a link between assets and container:
 *  1. Root asset links, which create top level Asset elements
 *  2. Parent/Child relationship between assets attached to a sub-tree Asset element parent,
 *  3. Asset/Container relationship between assets and container properties configured with destination point 
 * 	   references under the Asset Element parent
 */
public class LinkedDataMessage {
	/**
	 * <b>typeid</b>	ID of the static type used by the Assets.
	 * <b>values</b> 	Array of link object values.
	 */
	private String typeid;
	private ArrayList<LinkedValues> values;
	
	/**
	 * Constructor which sets the typeid and the array list of values of a linked data message
	 * @param typeid
	 * @param values
	 */
	public LinkedDataMessage(String typeid, ArrayList<LinkedValues> values) {
		this.typeid = typeid;
		this.values = values;
	}
}
