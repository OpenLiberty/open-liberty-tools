/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.junit.test;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.customization.ValidationMessage;
import com.ibm.xwt.dde.test.AttributeListValidation;
import com.ibm.xwt.dde.test.DDJUNITTestPlugin;
import com.ibm.xwt.dde.test.ElementListValidation;
import com.ibm.xwt.dde.test.ServletMappingSuggestedValues;

import junit.framework.TestCase;

public class DDETest extends TestCase{
	private File input = null;
	File input2 = null;
	private ElementListValidation elementValidation = null;
	private AttributeListValidation attributeValidation = null;
	private ServletMappingSuggestedValues suggestedValues = null;
	private Map<Node, ValidationMessage[]> msgs = null;
	public final String PLUGINID = "com.ibm.xwt.dde.junit.test";
		
	protected void setUp() throws Exception
	{
		elementValidation = new ElementListValidation();
		attributeValidation = new AttributeListValidation();
		suggestedValues = new ServletMappingSuggestedValues();
		String fileString = DDJUNITTestPlugin.getInstallURL() + "testresources/samples/WebApp/web.xml";
		input = new File(fileString);
	}
	//Test Ref: 85586
	@Test 
	public void testElementValidationMessages()throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		assertTrue(input.exists());
		Document document = dBuilder.parse(input);
		//ValidationManager manager = null;
		Element root = document.getDocumentElement();
		msgs = elementValidation.validate("anything",root,root, null);
		assertNotNull(msgs);
		Iterator<ValidationMessage[]> i = msgs.values().iterator();
		ValidationMessage[] msg = null;
		while(i.hasNext())
		{
			msg = (ValidationMessage[])i.next();
			int count = msg.length;
			for(int k = 0; k<count;k++)
			{
			String message = msg[k].getMessage();
			assertNotNull(message);
			assertTrue(message.startsWith("New Error message"));
			}
		}
		Node n = null;
		Iterator<Node> keys = msgs.keySet().iterator();
		while(keys.hasNext())
		{
			n = (Node)keys.next();
			assertTrue(n.getNodeType()!= Node.TEXT_NODE);
		}
	}
	
	//Test Ref: 85586
	@Test 
	public void testAttributeValidationMessages()throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		assertTrue(input.exists());
		Document document = dBuilder.parse(input);
		Element root = document.getDocumentElement();
		msgs = attributeValidation.validate("anything",root,root, null);
		assertNotNull(msgs);
		Iterator<ValidationMessage[]> i = msgs.values().iterator();
		ValidationMessage[] msg = null;
		while(i.hasNext())
		{
			msg = (ValidationMessage[])i.next();
			int count = msg.length;
			for(int k = 0; k<count;k++)
			{
			String message = msg[k].getMessage();
			assertNotNull(message);
			assertTrue(message.startsWith("New Error message"));
			}
		}
		Node n = null;
		Iterator<Node> keys = msgs.keySet().iterator();
		while(keys.hasNext())
		{
			n = (Node)keys.next();
			assertTrue(n.getNodeType()!= Node.TEXT_NODE);
		}
	}
	//Test Ref 84211
	@Test
	public void testSuggestedValues()throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		assertTrue(input.exists());
		Document document = dBuilder.parse(input);
		Element root = document.getDocumentElement();
		
		List<String> values = suggestedValues.getSuggestedValues("test", root, root, null);
		assertNotNull(values);
		assertFalse(values.isEmpty());
		assertTrue(values.size()==2);
		for(int j=0;j<2;j++)
		{
			assertTrue(((String)values.get(j)).contains("Faces Servlet"));
		}

	}
	
	//Test Ref 87590
	@Test
	public void testCustomControls()throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		assertTrue(input.exists());
		Document document = dBuilder.parse(input);
		Element root = document.getDocumentElement();
		
		List<String> values = suggestedValues.getSuggestedValues("test", root, root, null);
		assertNotNull(values);
		assertFalse(values.isEmpty());
		assertTrue(values.size()==2);
		for(int j=0;j<2;j++)
		{
			assertTrue(((String)values.get(j)).contains("Faces Servlet"));
		}

	}
}
