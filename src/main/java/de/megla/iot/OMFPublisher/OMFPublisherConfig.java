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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OMFPublisherConfig.java
 * 
 * @author Niklas Rose, Till Böcher  
 * This metatype annotation defines an object-class all the required
 * configurations for the options of a Cloud Publisher Wire Component.
 */
@ObjectClassDefinition(
		id = "de.megla.iot.OMFPublisher.OMFPublisher",
		name = "OMFPublisher Config",
		description= "Configurations for the OMFPublisher")
public @interface OMFPublisherConfig {	
	@AttributeDefinition(
			name = "producertoken",
			type = AttributeType.STRING)
	String producerToken();
	
	@AttributeDefinition(
			name = "targeturl",
			type = AttributeType.STRING)
	String targetURL();
	
	@AttributeDefinition(
			name = "devicename",
			type = AttributeType.STRING)
	String devicename();
	
	@AttributeDefinition(
			name = "sslverify",
			defaultValue = "true",
			type = AttributeType.BOOLEAN)
	boolean sslVerify();
	
	@AttributeDefinition(
			name = "connection.timeout.in.seconds",
			type = AttributeType.INTEGER,
			defaultValue = "5")
	int connectionTimeout();
	
	@AttributeDefinition(
			name = "in-flight.message.interval.in.milliseconds",
			type = AttributeType.INTEGER,
			defaultValue = "50")
	int inFlightInterval();
  
}