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
import java.util.HashMap;

/**
 * AssetFrameworkElement.java
 * 
 * In an on-premises destination system, an asset is interpreted as an Asset Element.
 */
public class AssetFrameworkElement {
	/**
	 * <b>typeid</b>	ID of the static type used by the Assets.
	 * <b>values</b> 	Array of Asset objects. Each object contains a key-value pairs representing property names 
	 * 					and their values of the static type used by the Asset.
	 */
	private String typeid;
	private ArrayList<HashMap<String, String>> values;
	
	/**
	 * Constructor which sets the typeid and the array list of values of an AF-element
	 */
	public AssetFrameworkElement(String typeid, ArrayList<HashMap<String, String>> values) {
		this.typeid = typeid;
		this.values = values;
	}
}
