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

package com.ibm.xwt.dde.internal.actions;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMContentBuilder;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMContentBuilderImpl;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.xwt.dde.customization.ICustomCreationObject;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.viewers.DDEViewer;

public class AddElementAction extends Action {

	private Element parentElement;
	private CMElementDeclaration cmElementDeclaration;
	private DDEViewer ddeViewer;
	private IEditorPart editorPart;
	private Image image;
	private Customization customization;
	
	
	public AddElementAction(Element parentElement, CMElementDeclaration cmElementDeclaration, DDEViewer ddeViewer, IEditorPart editorPart, Customization customization) {
		super(cmElementDeclaration.getElementName());
		this.parentElement = parentElement;
		this.cmElementDeclaration = cmElementDeclaration;
		this.ddeViewer = ddeViewer;
		this.editorPart = editorPart;
		this.customization = customization;
	}

	
	public void run() {
		if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
			String namespace = ModelUtil.getNamespaceURI(cmElementDeclaration);
			String path = ModelUtil.getNodeFullPath(parentElement, cmElementDeclaration);
			DetailItemCustomization itemCustomization = null;
			if(customization != null) {
				itemCustomization = customization.getItemCustomization(namespace, path);
				if(itemCustomization == null)
					itemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, path);
			}
			String itemLabel = null;
			Class creationClass = null;
			if(itemCustomization != null) {
				itemLabel = itemCustomization.getCreationLabel();
				if(itemLabel == null) {
					itemLabel = itemCustomization.getLabel();
				}
				creationClass = itemCustomization.getCreationClass();
			}
			if(itemLabel == null) {
				itemLabel = cmElementDeclaration.getElementName();
			}
			MessageFormat messageFormat = new MessageFormat(Messages.ADD);
			ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[] {itemLabel}));
			
			Element element = null;
			IDOMModel model = ((IDOMNode)parentElement).getModel();
			model.aboutToChangeModel();
			boolean parentNodeFormatRequired = false;
			boolean newLineRequired = false;
			
			// If the item has a custom creation object, invoke it
			if(creationClass != null) {
				Element[] elementTreeChildrenBeforeCustomCode = ModelUtil.getElementTreeChildren(customization, parentElement);
				try {
					Object object = creationClass.newInstance();
					if(object instanceof ICustomCreationObject) {
						ICustomCreationObject customCreationObject = (ICustomCreationObject)object;
						element = customCreationObject.create(parentElement, editorPart);
						// If the element returned is null, check to see if there is a new tree item added
						if(element == null) {
							Element[] elementTreeChildrenAfterCustomCode = ModelUtil.getElementTreeChildren(customization, parentElement);
							if(elementTreeChildrenBeforeCustomCode.length < elementTreeChildrenAfterCustomCode.length) {
								List elementsBeforeCustomCode = Arrays.asList(elementTreeChildrenBeforeCustomCode);
								List elementsAfterCustomCode = Arrays.asList(elementTreeChildrenAfterCustomCode);
								Iterator iterator = elementsAfterCustomCode.iterator();
								while(iterator.hasNext() && element == null) {
									Object currentObject = iterator.next();
									if(elementsBeforeCustomCode.indexOf(currentObject) == -1) {
										element = (Element)currentObject;
										newLineRequired = true;
									}
								}
							}
						} else {
							newLineRequired = true;
						}
					}
				} catch (Exception exception) {
					exception.printStackTrace();
				} 
	
			} else { // If there is no custom creation object associated with the item, create the element
				/*************************************************************************/
				ddeViewer.getTreeViewer().expandToLevel(parentElement, 1);
				ModelQuery modelQuery = ModelQueryUtil.getModelQuery(parentElement.getOwnerDocument());
				boolean append = false;
				Node refChild = parentElement.getLastChild();
				if(modelQuery.canInsert(parentElement, cmElementDeclaration, parentElement.getChildNodes().getLength(), ModelQuery.VALIDITY_STRICT)) {
					append = true;
				} else {
					Node targetChild = refChild;
					for(int i = parentElement.getChildNodes().getLength(); !modelQuery.canInsert(parentElement, cmElementDeclaration, i, ModelQuery.VALIDITY_STRICT) && i > 0; i--) {
						refChild = refChild.getPreviousSibling();
						// We don't want to insert before text nodes, otherwise the formatting will be messed up
						if (refChild.getNodeType() == Node.ELEMENT_NODE) {
						  targetChild = refChild;
						}
					}
					// update the refChild to be an Element
					refChild = targetChild;
					if(refChild == null) {
						refChild = parentElement.getFirstChild();
					}
				}
				Document document = parentElement.getNodeType() == Node.DOCUMENT_NODE ? (Document) parentElement : parentElement.getOwnerDocument();
				DOMContentBuilder domContentBuilder = new DOMContentBuilderImpl(document);
				
				//domContentBuilder.setBuildPolicy(DOMContentBuilder.BUILD_FIRST_CHOICE);
				if(document.getDocumentElement() != parentElement) {
					NodeList childNodes = parentElement.getChildNodes();
					parentNodeFormatRequired = childNodes == null || childNodes.getLength() == 0 || (childNodes.getLength() == 1 && (childNodes.item(0)).getNodeType() == Node.TEXT_NODE && ((childNodes.item(0)).getNodeValue().trim().length() == 0));
				}
				domContentBuilder.setBuildPolicy(0);
				domContentBuilder.build(parentElement, cmElementDeclaration);
				List list = domContentBuilder.getResult();
				for (Iterator i = list.iterator(); i.hasNext();) {
					Node newNode = (Node) i.next();
					if (newNode.getNodeType() == Node.ATTRIBUTE_NODE) {
						Element parent = (Element) parentElement;
						parent.setAttributeNode((Attr) newNode);
					}
					else {
						if(append) {
							parentElement.appendChild(newNode);
							newLineRequired = true;
						} else {
							parentElement.insertBefore(newNode, refChild);
						}
					}
				}
				if(list.size() > 0) {
					Object object = list.get(0);
					if(object instanceof Element) {
						element = (Element)object;
						ModelUtil.removeBlankCreatedRepeatableItems(element, customization);
						ModelUtil.assignDefaultValues(element, customization, editorPart);
					}
				}
				
			}
			if(element != null) {
				element = ModelUtil.recursivelyCompressEmptyElementTags(element);
				if(newLineRequired) {
					Text textNode = parentElement.getOwnerDocument().createTextNode(System.getProperty("line.separator"));
					parentElement.appendChild(textNode);
				}
				if(parentNodeFormatRequired) {
					ModelUtil.formatXMLNode(parentElement);
				} else {
					ModelUtil.formatXMLNode(element);
				}
				model.changedModel();
				ddeViewer.getValidationManager().validateTreeNode(element, true, false, false);
				ddeViewer.getValidationManager().validateTreeNode(parentElement, false, true, false);
				ddeViewer.updateValidationInformation();
				ddeViewer.getTreeViewer().setExpandedState(parentElement, true);
				ddeViewer.getTreeViewer().setExpandedState(element, true);
				ddeViewer.getTreeViewer().setSelection(new StructuredSelection(element), true);
				
			} else {
				model.changedModel();
			}
			ddeViewer.getUndoManager().endRecording(this);
		}
	}

	
	public void setImage(Image image) {
		this.image = image;
	}
	
	public Image getImage() {
		return image;
	}
	

	


}