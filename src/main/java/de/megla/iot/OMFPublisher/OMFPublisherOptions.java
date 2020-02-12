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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

/**
 * OMFPublisherOptions.java
 * 
 * The OMFPublisherOptions class supports the publisher by writing the individual definitions of Type, Container, 
 * and Data in a suitable JSON format. In addition, it creates the header according to the "messagetype" 
 * and makes a request to the destination system.
 */
public class OMFPublisherOptions{
	/**
	 * <b>properties</b>				properties which contain service configurations and user specific inputs
	 * <b>PRODUCER_TOKEN</b>			default value of the producertoken 
	 * <b>TARGET_URL</b>				default value of the target URL
	 * <b>DEVICENAME</b>				default value of the devicename
	 * <b>SSLVERIFY</b>					default value of the SSL verification
	 * <b>CONNECTIONTIMEOUT</b>			default value of the connection timeout
	 * <b>IN_FLIGHT_INTERVAl</b>		default value of the interval for sending a message which is currently in-flight
	 * <b>PRODUCER_TOKEN_NAME</b>		name of the property "producerToken"
	 * <b>TARGET_URL_NAME</b>			name of the property "targetURL"
	 * <b>DEVICENAME_NAME</b>			name of the property "devicename"
	 * <b>SSLVERIFY_NAME</b>			name of the property "sslVerify"
	 * <b>CONNECTIONTIMEOUT_NAME</b>	name of the property "connectionTimeout"
	 * <b>INFLIGHTINTERVAL_NAME</b>		name of the property "inFlightInterval"
	 */
	
	private final Map<String, Object> properties;
	
	private static final String PRODUCER_TOKEN = "";
    private static final String TARGET_URL = "";
    private static final String DEVICENAME = "";
    private static final Boolean SSLVERIFY = false;
    private static final int CONNECTIONTIMEOUT = 1;
    private static final int IN_FLIGHT_INTERVAl = 1;
    
    private static final String PRODUCER_TOKEN_NAME = "producerToken";
    private static final String TARGET_URL_NAME = "targetURL";
    private static final String DEVICENAME_NAME = "devicename";    
    private static final String SSLVERIFY_NAME = "sslVerify";
    private static final String CONNECTIONTIMEOUT_NAME = "connectionTimeout";
    private static final String INFLIGHTINTERVAL_NAME = "inFlightInterval";


    /**
     * Constructor which sets the properties of the options.
     */
    public OMFPublisherOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = properties;
    }
    
    /**
     * Returns the value of the producerToken, which is typed by a user.
     */
    public String getProducerToken() {
        String appId = PRODUCER_TOKEN;
        Object app = this.properties.get(PRODUCER_TOKEN_NAME);
        if (nonNull(app) && app instanceof String) {
            appId = String.valueOf(app);
        } //if
        return appId;
    }
    
    /**
     * Returns the value of the targetURL, which is typed by a user.
     */
    public String getTargetURL() {
        String appId = TARGET_URL;
        Object app = this.properties.get(TARGET_URL_NAME);
        if (nonNull(app) && app instanceof String) {
            appId = String.valueOf(app);
        } //if
        return appId;
    }
    
    /**
     * Returns the value of the devicename, which is typed by a user.
     */
    public String getDevicename() {
        String appId = DEVICENAME;
        Object app = this.properties.get(DEVICENAME_NAME);
        if (nonNull(app) && app instanceof String) {
            appId = String.valueOf(app);
        } //if
        return appId;
    }
    
    /**
     * Returns the value of the SSLVerify, which is typed by a user.
     */
    public boolean getSSLVerify() {
        boolean appId = SSLVERIFY;
        Object app = this.properties.get(SSLVERIFY_NAME);
        if (nonNull(app) && app instanceof Boolean) {
            appId = (boolean) app;
        } //if
        return appId;
    }
    
    /**
     * Returns the value of the connection Timeout for every message, which is typed by a user.
     * In short: How long should be waited till a connection timed out.
     */
    public int getConnectionTimeout() {
        int appId = CONNECTIONTIMEOUT;
        Object app = this.properties.get(CONNECTIONTIMEOUT_NAME);
        if (nonNull(app) && app instanceof Integer) {
            appId = (int) app;
        } //if
        return appId;
    }
    
    /**
     * Returns the value of the inFlightInterval for every message, which is typed by a user.
     * In short: After a period of time, send the next message.
     */
    public int getinFlightInterval() {
        int appId = IN_FLIGHT_INTERVAl;
        Object app = this.properties.get(INFLIGHTINTERVAL_NAME);
        if (nonNull(app) && app instanceof Integer) {
            appId = (int)app;
        } //if
        return appId;
    }
    
    /**
     * Checks if the current options are up-to-date.
     */
    public boolean compare(Object other) {
        if (!(other instanceof OMFPublisherOptions)) {
            return false;
        } //if
        OMFPublisherOptions that = (OMFPublisherOptions) other;

        // Custom equality check here.
        return this.getProducerToken().equals(that.getProducerToken())
            && this.getTargetURL().equals(that.getTargetURL())
            && this.getDevicename().equals(that.getDevicename())
            && !(this.getSSLVerify() && that.getSSLVerify());
    }
}
