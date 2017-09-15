/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.IEditorInput;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.xwt.dde.customization.ICustomListValidationObject;
import com.ibm.xwt.dde.customization.ValidationMessage;

public class AttributeListValidation implements ICustomListValidationObject {

	 public Map<Node, ValidationMessage[]> validate(String value, Node itemNode, Element closestAncestor, IEditorInput editorInput) {
		HashMap<Node, ValidationMessage[]> messages = new HashMap<Node, ValidationMessage[]>();
		String message = null;
		NodeList nodeList = closestAncestor.getChildNodes();
		Node node = null;
		for(int i = 0; i<nodeList.getLength();i++)
		{
			node = nodeList.item(i);
			NamedNodeMap attributes = null;
			if(node instanceof Element)
			{
				attributes = node.getAttributes();
				if(attributes != null)
				{
					int count = attributes.getLength();
					for(int k = 0;k<count;k++)
					{
						Attr attr = (Attr)attributes.item(k);
						node = attr;
					}
				}
			}
			if (node.getNodeType()== Node.ATTRIBUTE_NODE){
				message = "New Error message for attribute Node " + node.getNodeName()+"Belongs to Element " + ((Attr)node).getOwnerElement();
				messages.put(node, new ValidationMessage[]{new ValidationMessage(message, ValidationMessage.MESSAGE_TYPE_ERROR), new ValidationMessage("Another one", ValidationMessage.MESSAGE_TYPE_ERROR)});
			}

	}
		return messages;
	}
	
}

