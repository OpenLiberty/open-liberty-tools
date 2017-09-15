/*******************************************************************************
 * Copyright (c) 2001, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.data;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocumentation;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNodeList;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMNamespaceHelper;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xsd.contentmodel.internal.XSDImpl;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.util.XSDConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;

public class RepeatableAtomicDetailItemSet extends DetailItem {
	
	private Element grandParentElement;
	private Element parentElement;
	private CMElementDeclaration parentCMElementDeclaration;
	private CMElementDeclaration cmElementDeclaration;
	
	public String getName() {
		return cmElementDeclaration.getElementName();
	}
	
	public RepeatableAtomicDetailItemSet(Element parentElement, CMElementDeclaration elementDeclaration) {
		this.cmElementDeclaration = elementDeclaration;
		this.parentElement = parentElement;
	}
	
	public RepeatableAtomicDetailItemSet(Element grandParentElement, CMElementDeclaration parentCMElementDeclaration, CMElementDeclaration cmElementDeclaration) {
		this.cmElementDeclaration = cmElementDeclaration;
		this.grandParentElement = grandParentElement;
		this.parentCMElementDeclaration = parentCMElementDeclaration;
	}
	
	public AtomicDetailItem[] getItems() {
		List repeatableElements = new ArrayList();
		if(parentElement != null) {
			NodeList nodeList = parentElement.getChildNodes();
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
			for(int j = 0; j < nodeList.getLength(); j++) {
				Node node = nodeList.item(j);
				if(cmElementDeclaration.equals(modelQuery.getCMNode(node))) {
					AtomicElementDetailItem atomicElementDetailItem = new AtomicElementDetailItem((Element)node);
					atomicElementDetailItem.setDetailItemCustomization(detailItemCustomization);
					repeatableElements.add(atomicElementDetailItem);
				}
			}
		}
		return (AtomicDetailItem[]) repeatableElements.toArray(new AtomicDetailItem[repeatableElements.size()]);
	}
	
	public int getItemCount() {
		int items = 0;
		if(parentElement != null) {
			NodeList nodeList = parentElement.getElementsByTagNameNS(ModelUtil.getNodeNamespace(parentElement),cmElementDeclaration.getElementName());
			for(int j = 0; j < nodeList.getLength(); j++) {
				Node elementNode = nodeList.item(j);
				if(elementNode.getNodeType() == Node.ELEMENT_NODE && elementNode.getParentNode().equals(parentElement)) {
					items++;
				}
			}
		}
		return items;
	}
	
	public void moveDetailItem(AtomicDetailItem atomicDetailItem, boolean direction) {
		if(parentElement != null) {
			Element element = (Element) atomicDetailItem.getNode();
			if(element != null) {
				IDOMModel model = ((IDOMNode)element).getModel();
				model.aboutToChangeModel();
				Node sibling = null;
				if(direction) {
					Node previousNode = element.getPreviousSibling();
					while(previousNode != null && previousNode.getNodeType() != Node.ELEMENT_NODE) {
						previousNode = previousNode.getPreviousSibling();
					}
					if(previousNode != null) {
						parentElement.removeChild(element);
						parentElement.insertBefore(element, previousNode);
					}
					sibling = previousNode.getNextSibling();
				} else {
					Node nextNode = element.getNextSibling();
					while(nextNode != null && nextNode.getNodeType() != Node.ELEMENT_NODE) {
						nextNode = nextNode.getNextSibling();
					}
					if(nextNode != null) {
						parentElement.removeChild(nextNode);
						parentElement.insertBefore(nextNode, element);
					}
					sibling = element.getNextSibling();
				}
				if(sibling != null && sibling.getNodeType() == Node.TEXT_NODE) {
					String nodeValue = sibling.getNodeValue();
					if(nodeValue != null && nodeValue.trim().length() == 0) {
						parentElement.removeChild(sibling);
					}
				}
				ModelUtil.formatXMLNode(parentElement);
				model.changedModel();
			}
		}
	}

	
	public AtomicDetailItem addItem(String value) {
		if(parentElement == null) {
			parentElement = ModelUtil.obtainOrCreateElement(grandParentElement, parentCMElementDeclaration, detailItemCustomization != null? detailItemCustomization.getCustomization() : null);
			Text textNode = parentElement.getOwnerDocument().createTextNode(System.getProperty("line.separator"));
			parentElement.appendChild(textNode);
		}
		NodeList nodeList = parentElement.getElementsByTagNameNS(ModelUtil.getNodeNamespace(parentElement),cmElementDeclaration.getElementName());
		Node refChild = null;
		for(int j = 0; j < nodeList.getLength(); j++) {
			Node elementNode = nodeList.item(j);
			if(elementNode.getNodeType() == Node.ELEMENT_NODE && elementNode.getParentNode().equals(parentElement)) {
				refChild = elementNode.getNextSibling();
			}
		}
		if(refChild == null) {
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
			refChild = parentElement.getFirstChild();
			for(int i = 0; !modelQuery.canInsert(parentElement, cmElementDeclaration, i, ModelQuery.VALIDITY_STRICT) && i < parentElement.getChildNodes().getLength(); i++) {
				refChild = refChild.getNextSibling();
			}
		}
//		IDOMModel model = ((IDOMNode)parentElement).getModel();
//		model.aboutToChangeModel();
		boolean parentNodeFormatRequired = false;
		Document document = parentElement.getNodeType() == Node.DOCUMENT_NODE ? (Document) parentElement : parentElement.getOwnerDocument();
		if(document.getDocumentElement() != parentElement) {
			NodeList childNodes = parentElement.getChildNodes();
			parentNodeFormatRequired = childNodes == null || childNodes.getLength() == 0 || (childNodes.getLength() == 1 && (childNodes.item(0)).getNodeType() == Node.TEXT_NODE && ((childNodes.item(0)).getNodeValue().trim().length() == 0));
		}
		Element element = parentElement.getOwnerDocument().createElementNS(ModelUtil.getNodeNamespace(parentElement), DOMNamespaceHelper.computeName(cmElementDeclaration, parentElement, null));
		element.appendChild(parentElement.getOwnerDocument().createTextNode(value));
		parentElement.insertBefore(element, refChild);
		
		if(parentNodeFormatRequired) {
			ModelUtil.formatXMLNode(parentElement);
		} else {
			ModelUtil.formatXMLNode(element);
		}
//		model.changedModel();
		return new AtomicElementDetailItem(element);
	}
	
	public void removeItem(AtomicDetailItem atomicDetailItem) {
		if(parentElement!= null) {
			Element element = (Element) atomicDetailItem.getNode();
			if(element != null) {
				IDOMModel model = ((IDOMNode)parentElement).getModel();
				model.aboutToChangeModel();
				Node nextSibling = (Node)element.getNextSibling();
				parentElement.removeChild(element);
				ModelUtil.removeText(nextSibling);
				ModelUtil.formatXMLNode(parentElement);
				model.changedModel();
			}
		}
	}
	
	
	public boolean isRequired() {
		return cmElementDeclaration.getMinOccur() == 1;
	}
	
	public Element getClosestAncestor() {
		if(parentElement != null) {
			return parentElement;
		} else {
			return grandParentElement;
		}
	}
	//Getter to have the Element, currently we only have the ancestor
	public CMElementDeclaration getCMElementDeclaration()
	{
		return cmElementDeclaration;
	}
	
	public Map getPossibleValues() {
		String values[] = null;
		if(parentElement != null) {
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
			values = modelQuery.getPossibleDataTypeValues(parentElement, cmElementDeclaration);
		} else if(grandParentElement != null) {
			ModelQuery modelQuery = ModelQueryUtil.getModelQuery(grandParentElement.getOwnerDocument());
			values = modelQuery.getPossibleDataTypeValues(grandParentElement, cmElementDeclaration);
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

	//returns the documentation property of cmElementDeclaration and returns its value
	public String getDocumentation() {
		Object o = cmElementDeclaration.getProperty("documentation");
		if(o instanceof CMNodeList) {
			CMNodeList cmNodeList = (CMNodeList)o;
			for(int i = 0; i < cmNodeList.getLength(); i++) {
				CMNode cmNode = cmNodeList.item(i);
				if(cmNode.getNodeType() == CMNode.DOCUMENTATION) {
					CMDocumentation cmDoc = (CMDocumentation)cmNode;
					return cmDoc.getValue();
				}
			}
		}
		return null;
	}
	
	//gets DOM attributes from cmElementDeclaration, reads the "default" attribute,
	//and returns the message "Default: <default>"
	public String getElementDefaultAttribute() {
		//perform casts to XSDElementDeclaration so we can call element.getAttribute("default")
		if (cmElementDeclaration instanceof XSDImpl.XSDElementDeclarationAdapter) {
			XSDImpl.XSDElementDeclarationAdapter adapter = (XSDImpl.XSDElementDeclarationAdapter) cmElementDeclaration;
			Notifier target = adapter.getTarget();
			if (target instanceof XSDElementDeclaration) {
				XSDElementDeclaration edecl = (XSDElementDeclaration) target;
				
				String defaultValue = edecl.getElement().getAttribute(XSDConstants.DEFAULT_ATTRIBUTE);
				if(defaultValue != null && !"".equals(defaultValue)) {
					MessageFormat messageFormat = new MessageFormat(Messages.DEFAULT_VALUE);
					return messageFormat.format(new String[] {defaultValue});
				}
			}
		}
		return null;
	}
}
