/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
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

import org.eclipse.wst.xml.core.internal.contentmodel.CMDocumentation;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNodeList;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.internal.util.ModelUtil;

public class AtomicElementDetailItem extends AtomicDetailItem {

	private Element grandParentElement;
	private CMElementDeclaration parentCMElementDeclaration;
	private Element parentElement;
	private Element element;
	private CMElementDeclaration elementDeclaration;
	
	/**
	 * Constructor used when the element exists in the DOM document
	 * @param element
	 */
	public AtomicElementDetailItem(Element element) {
		this.element = element;
		Node parentNode = element.getParentNode();
		if(parentNode.getNodeType() == Node.ELEMENT_NODE) {
			this.parentElement = (Element) parentNode;	
		}
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
		if (modelQuery != null) {
			CMElementDeclaration elementDeclaration = modelQuery.getCMElementDeclaration(element);
			if (elementDeclaration != null) { 
				this.elementDeclaration = elementDeclaration;
			}
			CMElementDeclaration parentCMElementDeclaration = modelQuery.getCMElementDeclaration(parentElement);
			if (parentCMElementDeclaration != null) { 
				this.parentCMElementDeclaration = parentCMElementDeclaration;
			}
		}
	}
	
	/**
	 * Constructor used when the element does not exist in the DOM document, but its parent element does
	 * @param parentElement
	 * @param cmElementDeclaration
	 */
	public AtomicElementDetailItem(Element parentElement, CMElementDeclaration cmElementDeclaration) {
		this.parentElement = parentElement;
		this.elementDeclaration = cmElementDeclaration;
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
		CMElementDeclaration parentCMElementDeclaration = modelQuery.getCMElementDeclaration(parentElement);
		if (parentCMElementDeclaration != null) { 
			this.parentCMElementDeclaration = parentCMElementDeclaration;
		}
	}
	
	/**
	 * Constructor used when neither the element nor its parent element exist in the DOM document, but its grand parent element does
	 * 
	 * @param grandParentElement
	 * @param parentCMElementDeclaration
	 * @param cmElementDeclaration
	 */
	public AtomicElementDetailItem(Element grandParentElement, CMElementDeclaration parentCMElementDeclaration, CMElementDeclaration cmElementDeclaration) {
		this.grandParentElement = grandParentElement;
		this.parentCMElementDeclaration = parentCMElementDeclaration;
		this.elementDeclaration = cmElementDeclaration;
	}

	public Node getNode() {
		return this.element;
	}

	public Map getPossibleValues() {
		String values[] = null;
		if(element!= null) {
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
			values = modelQuery.getPossibleDataTypeValues(element, elementDeclaration);
		} else if(parentElement != null) {
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
			values = modelQuery.getPossibleDataTypeValues(parentElement, elementDeclaration);
		} else if(grandParentElement != null) {
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(grandParentElement.getOwnerDocument());
			values = modelQuery.getPossibleDataTypeValues(grandParentElement, elementDeclaration);
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
		boolean isCDATAStorage = isCDATASectionStorage();
		if (element != null) {
			for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
				if (!isCDATAStorage && node.getNodeType() == Node.TEXT_NODE) {
					String text = node.getNodeValue();
					if(!((text == null) || (text.trim().length() == 0))) {
						return text.trim();
					}
				} else if (isCDATAStorage && node.getNodeType() == Node.CDATA_SECTION_NODE) {
					String text = node.getNodeValue();
					if(!((text == null) || (text.trim().length() == 0))) {
						return text.trim();
					}
				}
			}
		}
		return "";
	}

	public boolean hasEditableValue() {
		int contentType = elementDeclaration.getContentType();
		if(contentType == CMElementDeclaration.MIXED || contentType == CMElementDeclaration.PCDATA) {
			return true;
		}
		return false;
	}


	public void setValue(String newValue) {
		boolean isCDATASectionStorage = isCDATASectionStorage();
		
		if(element == null) {
			if(parentElement == null) {
				parentElement = ModelUtil.obtainOrCreateElement(grandParentElement, parentCMElementDeclaration, detailItemCustomization != null? detailItemCustomization.getCustomization() : null);
			}
			element = ModelUtil.obtainOrCreateElement(parentElement, elementDeclaration, detailItemCustomization != null? detailItemCustomization.getCustomization() : null);
		}
		Node textNode = null;
		for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if(!isCDATASectionStorage && node.getNodeType() == Node.TEXT_NODE) {
				String text = node.getNodeValue();
				if(!((text == null) || (text.trim().length() == 0))) {
					textNode = node;
					break;
				}
			} else if(isCDATASectionStorage && node.getNodeType() == Node.CDATA_SECTION_NODE) {
				String text = node.getNodeValue();
				if(!((text == null) || (text.trim().length() == 0))) {
					textNode = node;
					break;
				}
			}
		}
		if(newValue != null) {
			if (textNode == null) {
				if(isCDATASectionStorage) {
					ModelUtil.removeCDATASection(element);
					textNode = element.getOwnerDocument().createCDATASection(newValue);
					element.appendChild(textNode);
				} else if(!"".equals(newValue)) {
						textNode = element.getOwnerDocument().createTextNode(newValue);
						element.appendChild(textNode);
				}
				ModelUtil.formatXMLNode(element);
			} else {
				if(!"".equals(newValue)) {
					textNode.setNodeValue(newValue);
				} else {
				  // REF 60371, 42482 - Empty elements, like, <a></a> have been
				  // simplified to the properly terminated short form: <a/> . The code that does this (in
				  // compressEmptyElementTag) rewrites the element.  The tree listener on the left side detects that
				  // the element is gone, and refreshes (it loses focus as a side effect).
				  // 
				  // This situation only happens for customizations where the element in the document appears
				  // in BOTH the left side tree and the right detailed side as a widget, as in this case, a text field.
				  //
				  // In these cases, to prevent the refresh, the empty element will NOT be compacted to the short form.
					if(detailItemCustomization != null && 
					    parentElement != null && 
					    !ModelUtil.elementMustAppearInTree(detailItemCustomization.getCustomization(), parentElement, parentCMElementDeclaration, elementDeclaration)) {
						element.removeChild(textNode);
						element = ModelUtil.compressEmptyElementTag(element);
						ModelUtil.formatXMLNode(element);
					} else {
						textNode.setNodeValue("");
					}
				}
			}
		}
	}


	public String getName() {
		return elementDeclaration.getElementName();
	}

	public boolean isRequired() {
		return elementDeclaration.getMinOccur() == 1;
	}
	
	public boolean isOptionalWithinContext() {
		int condition = ModelUtil.CHOICE | ModelUtil.OPTIONAL;
		return (ModelUtil.getGroupTypesInBetween(parentCMElementDeclaration, elementDeclaration) & condition) == condition;
	}

	public boolean exists() {
		return element != null;
	}

	public void delete() {
		if(element != null) {
//			IDOMModel model = ((IDOMNode)element).getModel();
//			model.aboutToChangeModel();
			ModelUtil.removePrecedingText(element);
			parentElement.removeChild(element);
			element = null;
//			model.changedModel();
		}
	}

	
	public String getDocumentation() {
		Object object = elementDeclaration.getProperty("documentation");
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
		if(parentElement != null) {
			return parentElement;
		}
		return grandParentElement;
	}

	public CMNode getCMNode() {
		return elementDeclaration;
	}
	
	private boolean isCDATASectionStorage() {
		if(detailItemCustomization != null) {
			return detailItemCustomization.isCDATASectionStorage();
		}
		return false;
	}

}
