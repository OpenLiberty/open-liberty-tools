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
package com.ibm.xwt.dde.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class ResetCustomizations implements IEditorActionDelegate {

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
	}

	public void run(IAction action) {
		//CustomizationManager.getInstance().resetCustomizations();
		IWorkbenchPart workbenchPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if(workbenchPart instanceof DDEMultiPageEditorPart) {
			DDEMultiPageEditorPart ddeMultiPageEditorPart = (DDEMultiPageEditorPart)workbenchPart;
			ddeMultiPageEditorPart.resetCustomizations();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}