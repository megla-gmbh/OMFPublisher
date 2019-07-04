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

/**
 * SourceTarget.java
 *
 * @author Niklas Rose, Till B�cher 
 * Representation of Source and Target object for three different types of links:
 * 1.	The Root asset 
 * 2.	Parent/Child relationship between assets attached to a sub-tree asset element parent.
 * 3. 	Asset/Container relationship between assets and container properties configured with destination point 
 * 		references under the asset element parent
 */
public class SourceTarget {
	/**
	 * <b>typeid</b> 		1. 		ID of the static type definition used by the asset.
	 * 						2./3. 	ID of the static type definition used by the asset -> a parent of the target asset.
	 * <b>containerid</b> 	2./3.	ID of the container created from dynamic type definition.
	 * <b>index</b>			1.		(Source) Value must be set to _ROOT. (Target) Asset name value at its creation to isindex property.
	 * 						2./3.	Asset name value at its creation to isindex property.
	 * <b>typeversion</b>	1./2./3. Optional version of the type. If omitted version 1.0.0.0 is assumed.
	 */
	private String typeid;
	private String containerid;
	private String index;
	private String typeversion;
	
	/**
	 * Constructor which sets typeid, containerid, index and typeversion of a source or target object
	 * @param typeid
	 * @param containerid
	 * @param index
	 * @param typeversion
	 */
	public SourceTarget(String typeid, String containerid, String index, String typeversion) {
		this.typeid = typeid;
		this.containerid = containerid;
		this.index = index;
		this.typeversion = typeversion;
	}
	
	/**
	 * Constructor which sets typeid and index of a source or target object
	 * @param typeid
	 * @param index
	 */
	public SourceTarget(String typeid, String index) {
		this.typeid = typeid;
		this.index = index;
	}

	/**
	 * Constructor which sets the containerid of a source or target object
	 * @param containerid
	 */
	public SourceTarget(String containerid) {
		this.containerid = containerid;
	}
	
}