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
package com.ibm.xwt.dde.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.customization.ICustomSuggestedValuesObject;

public class ServletMappingSuggestedValues implements ICustomSuggestedValuesObject {

	public List<String> getSuggestedValues(String value, Node itemNode, Element closestAncestor, IResource resource) {
		// Collect the names of all the servlets in the document and return a Map with it
		Map<String, String> map = new HashMap<String, String>();
		Element webAppElement = closestAncestor.getOwnerDocument().getDocumentElement();
		for(Node node = webAppElement.getFirstChild(); node != null; node = node.getNextSibling()) {
			if("servlet".equals(node.getNodeName())) {
				for(Node servletChildNode = node.getFirstChild(); servletChildNode != null; servletChildNode = servletChildNode.getNextSibling()) {
					if("servlet-name".equals(servletChildNode.getNodeName())) {
						for (Node servletNameChildNode = servletChildNode.getFirstChild(); servletNameChildNode != null; servletNameChildNode = servletNameChildNode.getNextSibling()) {
							if (servletNameChildNode.getNodeType() == Node.TEXT_NODE) {
								String text = servletNameChildNode.getNodeValue();
								if(!(text == null)) {
									text = text.trim();
									map.put(text, text);
									map.put("second "+text,"second "+text);
								}
							}
						}
					}
				}
			}
		}
		List<String> myList = new ArrayList<String>(map.keySet());
		return myList;
	}
}
