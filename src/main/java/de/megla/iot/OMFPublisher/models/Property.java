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

import com.google.gson.annotations.SerializedName;

/**
 * Property.java
 *
 * @author Niklas Rose, Till Böcher 
 * Represents one Property in the Type defintion.
 * Types are listed at the URL: https://omf-developers-companion.readthedocs.io/en/latest/OMF_Type_msg.html#property-types-and-formats
 */
public class Property {
	/**
	 * <b>type</b>			Required type of the Type Property 
	 * <b>format</b>		Optional format of the Type Property 
	 * <b>isindex</b>		One Property must be designated as the index with the value "true"
	 * <b>isname</b>		One Property may be optionally designated as the name the value "true".
	 * <b>enum</b>			Optional enumeration. Contains an array of values. The type depends on the keyword type. 
	 * <b>name</b>			Optional name for the Property.
	 * <b>description</b>	Optional description for the Property.
	 * <b>items</b>			Must be defined if an array-type is used.
	 * <b>maxItems</b>		The exact amount of array-elements which needed
	 * <b>additionalProperties</b> some additional properties for nested objects
	 */
	enum BoolEnum{
		 @SerializedName("True") True,
	     @SerializedName("False") False
	}
	private String type;
	private String format;
	private boolean isindex;
	private boolean isname;
	@SerializedName("enum") 
	private BoolEnum[] boolEnum;
	private String name;
	private String description;
	private Property items;
	private int maxItems;
	private Property additionalProperties;

	/**
	 * Constructor which sets the type, format and isindex of an property element
	 * @param type
	 * @param format
	 * @param isindex
	 */
	public Property(String type, String format, boolean isindex) {
		this.type = type;
		this.format = format;
		this.isindex = isindex;
	}
	
	/**
	 * Constructor which sets the type and isindex of an property element
	 * @param type
	 * @param isindex
	 */
	public Property(String type, boolean isindex) {
		this.type = type;
		this.isindex = isindex;
	}

	/**
	 * Constructor which sets the type and boolEnum of an property element
	 * @param type
	 * @param bool
	 */
	public Property(String type, BoolEnum[] bool) {
		this.type = type;
		this.boolEnum = bool;
	}
	
	/**
	 * Constructor which sets the type, items and maxItems of an property element
	 * @param type
	 * @param items
	 * @param maxItems
	 */
	public Property(String type, Property items, int maxItems) {
		this.type = type;
		this.items = items;
		this.maxItems = maxItems;
	}
	
	/**
	 * Constructor which sets the type of an property element
	 * @param type
	 */
	public Property(String type) {
		this.type = type;
	}

}
