/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.data;

import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SimpleDetailItem extends DetailItem {

	private DetailItem[] atomicDetailItems;
	private CMElementDeclaration cmElementDeclaration;
	private Element element;
	
	public SimpleDetailItem(CMElementDeclaration cmElementDeclaration, DetailItem[] atomicDetailItems) {
		this.cmElementDeclaration = cmElementDeclaration;
		this.atomicDetailItems = atomicDetailItems;
	}
	
	public SimpleDetailItem(CMElementDeclaration cmElementDeclaration, Element element, DetailItem[] atomicDetailItems) {
		this.cmElementDeclaration = cmElementDeclaration;
		this.element = element;
		this.atomicDetailItems = atomicDetailItems;
	}

	public String getName() {
		return cmElementDeclaration.getElementName();
	}
	
	public boolean isRequired() {
		return cmElementDeclaration.getMinOccur() == 1;
	}

	public DetailItem[] getAtomicDetailItems() {
		return atomicDetailItems;
	}

	public boolean isRepeatable() {
		return false;
	}
	
	public Element getElement() {
		if(element != null) {
			if(element.getParentNode() != null) {
				return element;
			} else {
				return null;
			}
		} else {
			Element element = null;
			int i = 0;
			while(element == null && i < atomicDetailItems.length) {
				if(atomicDetailItems[i] instanceof AtomicDetailItem) {
					AtomicDetailItem atomicDetailItem = (AtomicDetailItem) atomicDetailItems[i];
					Node node = atomicDetailItem.getNode();
					if(node != null) {
						if(node.getNodeType() == Node.ATTRIBUTE_NODE) {
							Attr attr = (Attr)node;
							element = attr.getOwnerElement();
						} else if(node.getNodeType() == Node.ELEMENT_NODE) {
							node = node.getParentNode();
							element = (Element)node;
						}
					}
				} else if(atomicDetailItems[i] instanceof RepeatableAtomicDetailItemSet) {
					RepeatableAtomicDetailItemSet repeatableAtomicDetailItemSet = (RepeatableAtomicDetailItemSet)atomicDetailItems[i];
					AtomicDetailItem[] items = repeatableAtomicDetailItemSet.getItems();
					if(items.length > 0) {
						AtomicDetailItem atomicDetailItem = items[0];
						Node node = atomicDetailItem.getNode();
						node = node.getParentNode();
						if(node.getNodeType() == Node.ELEMENT_NODE) {
							element = (Element)node;
						}
					}
				}
				i++;
			}
			if(element != null && element.getParentNode() != null) {
				this.element = element;
			}
			return element;
		}
	}
	

}
