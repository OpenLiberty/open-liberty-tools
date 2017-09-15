/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.internal.viewers;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.util.ModelUtil;

public class TreeContentProvider implements ITreeContentProvider {

	protected Viewer viewer;
	
	private Customization customization;
	
	public TreeContentProvider(Customization customization) {
		this.customization = customization;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
	}

	public Object[] getChildren(Object parentObject) {
		if (parentObject instanceof Node) {
			Node parentNode = (Node)parentObject;
			ArrayList list = new ArrayList();
			// When the parent is the document node, include all its children
			if(parentNode.getNodeType() == Node.DOCUMENT_NODE) {
				for (Node childNode = parentNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
					if(childNode.getNodeType() == Node.ELEMENT_NODE) {
						Element childElement = (Element)childNode;
						DetailItemCustomization detailItemCustomization = getDetailItemCustomization(childElement);
						if(!(detailItemCustomization != null && detailItemCustomization.isHidden())) {
							list.add(childNode);
						}
					}
				}
			} else if(parentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element parentElement = (Element)parentNode;
				ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
				if(modelQuery != null) {
					CMElementDeclaration parentCMElementDeclaration = modelQuery.getCMElementDeclaration(parentElement);
					for (Node childNode = parentNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
						if (childNode.getNodeType() == Node.ELEMENT_NODE) {
							Element childElement = (Element)childNode;
							CMElementDeclaration childCMElementDeclaration = modelQuery.getCMElementDeclaration(childElement);
							if(parentCMElementDeclaration != null && childCMElementDeclaration != null) {
//								DetailItemCustomization detailItemCustomization = getDetailItemCustomization(childElement);
								// Determine if the element should appear in the tree
								if(ModelUtil.elementMustAppearInTree(customization, parentElement, parentCMElementDeclaration, childCMElementDeclaration)) {
									list.add(childNode);
								}
							} 
						}
					}
				}
			}
			return list.toArray();
		}
		return Collections.EMPTY_LIST.toArray();
	}

	public boolean hasChildren(Object element) {
		Object[] children = getChildren(element);
		return children.length > 0;
	}

	public Object getParent(Object element) {
		return null;
	}

	public Object[] getElements(java.lang.Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
	}
	
	private DetailItemCustomization getDetailItemCustomization(Element element) {
		DetailItemCustomization itemCustomization = null;
		if(customization != null) {
			String path = ModelUtil.getElementFullPath(element);
			String elementNamespace = ModelUtil.getNodeNamespace(element);
			itemCustomization = customization.getItemCustomization(elementNamespace, path);
			if( itemCustomization == null)
			{
				ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
				if (modelQuery != null) {
					CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
					itemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, path);
				}				
			}
		}
		return itemCustomization;
	}

}