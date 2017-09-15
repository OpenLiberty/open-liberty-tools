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
package com.ibm.xwt.dde.internal.viewers;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.w3c.dom.Element;

import com.ibm.xwt.dde.DDEPlugin;
import com.ibm.xwt.dde.internal.actions.MoveAction;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;

public class TreeItemMenuListener implements IMenuListener {

	private DDEViewer ddeViewer;
	private Customization customization;
	private IEditorPart editorPart;

	
	public TreeItemMenuListener(DDEViewer ddeViewer, Customization customization, IEditorPart editorPart) {
		this.ddeViewer = ddeViewer;
		this.customization = customization;
		this.editorPart = editorPart;
	}


	public void menuAboutToShow(IMenuManager manager) {
		ISelection selection = ddeViewer.getTreeViewer().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.getFirstElement() instanceof Element) {
				Element element = (Element)structuredSelection.getFirstElement();
				fillContextMenu(manager, element);
				manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
			}
		}
	}

	
	private void fillContextMenu(IMenuManager manager, Element element) {
		// Add insert actions
		if(!ddeViewer.isReadOnlyMode() && ddeViewer.getTreeViewer().getTree().getSelection().length == 1) {
			List insertActions = ModelUtil.getInsertActions(element, customization, ddeViewer, editorPart);
	
			if(insertActions.size() > 0) {
				MenuManager insertSubMenu = new MenuManager(Messages.ADD_WITH_MNEMONIC, DDEPlugin.getDefault().getImageDescriptor("icons/add.gif"), null);			
				Action insertActionsArray[] = (Action[])insertActions.toArray(new Action[insertActions.size()]);
				Comparator comparator = new Comparator() {
					public int compare(Object arg0, Object arg1) {
						Action a = (Action)arg0;
						Action b = (Action)arg1;
						return Collator.getInstance().compare(a.getText(), b.getText());
					}
				};
				Arrays.sort(insertActionsArray, comparator);
				for (int i = 0; i < insertActionsArray.length; i++) {
					insertSubMenu.add(insertActionsArray[i]);
				}
				manager.add(insertSubMenu);		
			}
		}
		
		// Add delete action
		////Action deletionAction = ModelUtil.getDeletionAction(element, ddeViewer, customization, editorPart);
		Action deletionAction = new Action(){
			
			public void run() {
				ddeViewer.performDelete();
			}
		};

		boolean enableRemove = true;
		TreeItem[] treeItems = ddeViewer.getTreeViewer().getTree().getSelection();
		for (int i = 0; i < treeItems.length && enableRemove; i++) {
			Object object = treeItems[i].getData();
			if(object instanceof Element) {
				Element currentElement = (Element)object;
				enableRemove = ModelUtil.canDeleteElement(currentElement, customization, editorPart);
			}
		}
		deletionAction.setEnabled(enableRemove);
		deletionAction.setImageDescriptor(DDEPlugin.getDefault().getImageDescriptor("icons/remove.gif"));
		deletionAction.setText(Messages.REMOVE_WITH_MNEMONIC);
		
		
		
		Separator separator = new Separator();
		MoveAction moveUpAction = new MoveAction(ddeViewer, true);
		MoveAction moveDownAction = new MoveAction(ddeViewer, false);

		if(ddeViewer.isReadOnlyMode()) {
			deletionAction.setEnabled(false);
			moveUpAction.setEnabled(false);
			moveDownAction.setEnabled(false);
		}
		
		manager.add(deletionAction);
		manager.add(separator);
		manager.add(moveUpAction);
		manager.add(moveDownAction);
	}
}