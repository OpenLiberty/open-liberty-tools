/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.data;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocumentation;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNodeList;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.internal.util.ModelUtil;

public class AtomicAttributeDetailItem extends AtomicDetailItem {

	private Element parentElement;
	private CMElementDeclaration cmElementDeclaration;
	private Element element;
	private CMAttributeDeclaration attributeDeclaration;
	
	public AtomicAttributeDetailItem(Element element, CMAttributeDeclaration attributeDeclaration) {
		this.element = element;
		this.attributeDeclaration = attributeDeclaration;
	}
	
	public AtomicAttributeDetailItem(Element parentElement, CMElementDeclaration cmElementDeclaration, CMAttributeDeclaration attributeDeclaration) {
		this.parentElement = parentElement;
		this.cmElementDeclaration = cmElementDeclaration;
		this.attributeDeclaration = attributeDeclaration;
	}
	
	public Map getPossibleValues() {
		String values[] = null;
		ModelQuery modelQuery = null;
		if(element != null) {
			modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
			values = modelQuery.getPossibleDataTypeValues(element, attributeDeclaration);
		} else if(parentElement != null) {
			modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
			values = modelQuery.getPossibleDataTypeValues(parentElement, attributeDeclaration);
		}
		if(values.length > 1) {
			Map map = new LinkedHashMap();
			for (int i = 0; i < values.length; i++) {
				map.put(values[i], values[i]);
			}
			return map;
		}
		return null;
	}

	public String getValue() {
        if(element != null) {
			String value = element.getAttribute(attributeDeclaration.getNodeName());
	        if(value != null) {
	        	return value;
	        }
        }
        return "";
	}

	public boolean hasEditableValue() {
		return true;
	}

	public void setValue(String newValue) {
		if(element == null) {
			element = ModelUtil.obtainOrCreateElement(parentElement, cmElementDeclaration, detailItemCustomization != null? detailItemCustomization.getCustomization() : null);
		}
//			IDOMModel model = ((IDOMNode)element).getModel();
//			model.aboutToChangeModel();
		element.setAttribute(attributeDeclaration.getAttrName(), newValue);
		if(element.getParentNode().getNodeType() != Node.DOCUMENT_NODE) {
//				ModelUtil.formatXMLNode(element);
		}
//			model.changedModel();
	}

	
	public String getName() {
		return attributeDeclaration.getNodeName();
	}
	
	public Node getNode() {
		if(element!= null) {
			return element.getAttributeNode(attributeDeclaration.getNodeName());
		}
		return null;
	}

	public boolean isRequired() {
		return attributeDeclaration.getUsage() == CMAttributeDeclaration.REQUIRED;
	}

	public boolean exists() {
		if(element != null) {
			return element.hasAttribute(attributeDeclaration.getNodeName());
		}
        return false;
	}

	public void delete() {
//		IDOMModel model = ((IDOMNode)element).getModel();
//		model.aboutToChangeModel();
		element.removeAttribute(attributeDeclaration.getAttrName());
//		ModelUtil.formatXMLNode(element);
//		model.changedModel();
	}
	
	public String getDocumentation() {
		Object object = attributeDeclaration.getProperty("documentation");
		if(object instanceof CMNodeList) {
			CMNodeList cmNodeList = (CMNodeList)object;
			int n = cmNodeList.getLength();
			String documentation = "";
			for (int i = 0; i < n; i++) {
				CMNode cmNode = cmNodeList.item(i);
				if(cmNode.getNodeType() == CMNode.DOCUMENTATION) {
					CMDocumentation cmDocumentation = (CMDocumentation)cmNode;
					documentation += cmDocumentation.getValue();
				}
			}
			return documentation;
		}
		return null;
	}

	public Element getClosestAncestor() {
		if(element != null) {
			return element;
		}
		return parentElement;
	}

	public CMNode getCMNode() {
		return attributeDeclaration;
	}

	public boolean isOptionalWithinContext() {
		return false;
	}

}
