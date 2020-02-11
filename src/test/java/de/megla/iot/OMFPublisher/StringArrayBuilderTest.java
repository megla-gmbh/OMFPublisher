/*
    StringArrazBuilderTest.java
    Copyright (c) 2020 MEGLA GmbH. All rights reserved.
*/
package de.megla.iot.OMFPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringArrayBuilderTest {
	@Test
	public void getArrayString_NoContent_ReturnsEmptyArray() {
		StringArrayBuilder builder = new StringArrayBuilder();
		
		assertEquals("[]", builder.getArrayString());
	}
	
	@Test
	public void getArrayString_OneContent_ReturnsArrayWithOneMember() {
		StringArrayBuilder builder = new StringArrayBuilder();
		
		builder.addContent("a");
		
		assertEquals("[a]", builder.getArrayString());
	}
	
	@Test
	public void getArrayString_MultiContent_ReturnsArray() {
		StringArrayBuilder builder = new StringArrayBuilder();
		
		builder.addContent("a");
		builder.addContent("b");
		builder.addContent("c");
		
		assertEquals("[a,b,c]", builder.getArrayString());
	}
}
