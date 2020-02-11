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

import java.util.LinkedList;
import java.util.Queue;

/**
 * StringArrayBuilder.java
 * 
 * @author Niklas Rose, Till Böcher
 */
public class StringArrayBuilder {
	private Queue<String> contents = new LinkedList<String>();
	
	/**
	 * Adds a new elements to the string array.
	 */
	public void addContent(String content) {
		this.contents.add(content);
	}
	
	/**
	 * Builds an array in the format [a,b,c,...] from all content inserted.
	 */
	public String getArrayString() {
		StringBuilder result = new StringBuilder();
		
		result.append("[");
		while(!this.contents.isEmpty()) {
			result.append(contents.remove());
			if(!contents.isEmpty())
				result.append(",");
		}
		
		result.append("]");
		
		return result.toString();
	}
}
