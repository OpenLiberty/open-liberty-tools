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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.customization.ICustomDeletionObject;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.validation.ValidationManager;
import com.ibm.xwt.dde.internal.viewers.DDEViewer;

public class DeleteElementAction extends Action {

	private Element element;
	private DDEViewer ddeViewer;
	private IEditorPart editorPart;
	private Class deletionClass;

	public DeleteElementAction(Element element, DDEViewer viewer, IEditorPart editorPart) {
		super(Messages.REMOVE_WITH_MNEMONIC);
		this.element = element;
		this.ddeViewer = viewer;
		this.editorPart = editorPart;
	}

	public void run() {
		if(((DDEMultiPageEditorPart)editorPart).validateEditorInput()) {
			//String namespace = element.getNamespaceURI();
			String namespace = ModelUtil.getNodeNamespace(element);
			String path = ModelUtil.getElementFullPath(element);
			Customization customization = ddeViewer.getCustomization();
			DetailItemCustomization itemCustomization = null;
			if(customization != null) {
				itemCustomization = customization.getItemCustomization(namespace, path);
				if(itemCustomization == null)
				{
					ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
					if (modelQuery != null) {
						CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);
						itemCustomization = customization.getTypeCustomizationConsideringUnions(cmElementDeclaration, path);
					}
				}
			}
			String itemLabel = null;
			if(itemCustomization != null) {
				itemLabel = itemCustomization.getLabel();
			}
			if(itemLabel == null) {
				itemLabel = element.getNodeName();
			}
			MessageFormat messageFormat = new MessageFormat(Messages.REMOVE);
			ddeViewer.getUndoManager().beginRecording(this, messageFormat.format(new String[] {itemLabel}));
			
			// To ensure consistency, the root element reference must be obtained before any call to custom code.
			Element documentElement = element.getOwnerDocument().getDocumentElement();
			
			boolean refresh = false;
			Node parentNode = element.getParentNode();
			if(deletionClass != null) {
				try {
					Object object = deletionClass.newInstance();
					if(object instanceof ICustomDeletionObject) {
						ICustomDeletionObject customDeletionObject = (ICustomDeletionObject)object;
						IDOMModel model = ((IDOMNode)element).getModel();
						model.aboutToChangeModel();
						refresh = customDeletionObject.delete(element, editorPart);
						model.changedModel(); 
					}
				} catch (Exception exception) {
					exception.printStackTrace();
				} 
			} else {
				IDOMModel model = ((IDOMNode)element).getModel();
				model.aboutToChangeModel();
				ModelUtil.removePrecedingText(element);
				parentNode.removeChild(element);
				model.changedModel();
				refresh = true;
			}
			if(refresh) {
				ValidationManager validationManager = ddeViewer.getValidationManager();
				if(parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE && parentNode.getParentNode() != null) {
					Element parentElement = (Element)parentNode;
					validationManager.getMessageManager().removeTreeNode(parentElement, element);
					validationManager.validateTreeNode(parentElement, false, false, false);
					ddeViewer.updateValidationInformation();
					ddeViewer.setSelection(new StructuredSelection(parentNode), true);
				} else {
					validationManager.validateTreeNode(documentElement, true, false, false);
					ddeViewer.updateValidationInformation();
					ddeViewer.setSelection(new StructuredSelection(documentElement), true);
				}
				
			}
			ddeViewer.getUndoManager().endRecording(this);
		}
	}
	
	public void setDeletionClass(Class deletionClass) {
		this.deletionClass = deletionClass;
	}
}