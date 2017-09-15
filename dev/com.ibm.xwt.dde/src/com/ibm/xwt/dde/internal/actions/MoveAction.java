/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.DDEPlugin;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;
import com.ibm.xwt.dde.internal.messages.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;
import com.ibm.xwt.dde.internal.viewers.DDEViewer;

public class MoveAction extends Action {
	
	private DDEViewer ddeViewer;
	private boolean direction;
	
	public MoveAction(DDEViewer ddeViewer, boolean direction) {
		this.ddeViewer = ddeViewer;
		this.direction = direction;

		boolean enabled = false;
		TreeItem[] treeItems = ddeViewer.getTreeViewer().getTree().getSelection();
		if(treeItems.length == 1) {
			TreeItem treeItem = treeItems[0];
			TreeItem parentTreeItem = treeItem.getParentItem();
			if(ddeViewer.getTreeViewer().getSorter() == null) {
				if(parentTreeItem != null) {
					TreeItem[] childItems = parentTreeItem.getItems();
					if(direction && !childItems[0].equals(treeItem)) {
						enabled = true;
					}
					if(!direction && !childItems[childItems.length - 1].equals(treeItem)) {
						enabled = true;
					}
				}
			}
		}
		this.setEnabled(enabled);
		
		if(direction) {
			this.setImageDescriptor(DDEPlugin.getDefault().getImageDescriptor("icons/move_up.gif"));
			this.setText(Messages.LABEL_MOVE_UP_WITH_MNEMONIC);
		} else {
			this.setImageDescriptor(DDEPlugin.getDefault().getImageDescriptor("icons/move_down.gif"));
			this.setText(Messages.LABEL_MOVE_DOWN_WITH_MNEMONIC);
		}

	}

	public void run() {
		if(((DDEMultiPageEditorPart)ddeViewer.getEditorPart()).validateEditorInput()) {
			IStructuredTextUndoManager undoManager = ddeViewer.getUndoManager();
			TreeViewer treeViewer = ddeViewer.getTreeViewer();
			TreeItem treeItem = treeViewer.getTree().getSelection()[0];
			TreeItem parentTreeItem = treeItem.getParentItem();
			TreeItem[] childItems = parentTreeItem.getItems();
			Element source = (Element)treeItem.getData();
			Element parentElement = (Element)source.getParentNode();
			IDOMModel model = ((IDOMNode)source).getModel();
			Node sibling = null;
			if(direction) {
				int i = 1;
				while(!childItems[i].equals(treeItem) && i < childItems.length) {
					i++;
				}
				Element target = (Element)childItems[i - 1].getData();
				MessageFormat messageFormat = new MessageFormat(Messages.MOVE_UP);
				undoManager.beginRecording(this, messageFormat.format(new String[]{treeItem.getText()}));
				model.aboutToChangeModel();
				parentElement.replaceChild(target, source);
				parentElement.insertBefore(source, target);
				sibling = source.getPreviousSibling();
			} else {
				int i = childItems.length - 1;
				while(!childItems[i].equals(treeItem) && i > 0) {
					i--;
				}
				Element target = (Element)childItems[i + 1].getData();
				MessageFormat messageFormat = new MessageFormat(Messages.MOVE_DOWN);
				undoManager.beginRecording(this, messageFormat.format(new String[]{treeItem.getText()}));
				model.aboutToChangeModel();
				parentElement.replaceChild(source, target);
				parentElement.insertBefore(target, source);
				sibling = target.getPreviousSibling();
			}
			if(sibling != null && sibling.getNodeType() == Node.TEXT_NODE) {
				String nodeValue = sibling.getNodeValue();
				if(nodeValue != null && nodeValue.trim().length() == 0) {
					parentElement.removeChild(sibling);
				}
			}
			ModelUtil.formatXMLNode(source);
			model.changedModel();
			undoManager.endRecording(this);
			ISelection selection = new StructuredSelection(source);
			treeViewer.refresh();
			treeViewer.setSelection(selection);
		}
	}

}
