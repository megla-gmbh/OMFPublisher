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
 * LinkedValues.java
 *
 * Each link object has the properties "Source" and "Target".
 */
public class LinkedValues {
	/**
	 * <b>Source</b> An object representing the source of the link or its parent.
	 * <b>Target</b> An object representing the target of the link or its child.
	 */
	private SourceTarget Source;
	private SourceTarget Target;
	
	/**
	 * Constructor which sets the source and target of the values, which are linked
	 * @param source
	 * @param target
	 */
	public LinkedValues(SourceTarget source, SourceTarget target) {
		this.Source = source;
		this.Target = target;
	}
}
