/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.xwt.dde.test;

import java.text.MessageFormat;

import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.customization.ICustomElementListSelectionDialog;


@SuppressWarnings("restriction")
public class CustomizedAddExtendor implements ICustomElementListSelectionDialog {
	
	public void invoke(Element element)
	{
		System.err.println("Adding Child elements for:  = " + element.getLocalName());
		
		IRunnableContext context= new BusyIndicatorRunnableContext();
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		int style= IJavaElementSearchConstants.CONSIDER_ALL_TYPES;
		try {
			SelectionDialog dialog= JavaUI.createTypeDialog(Display.getDefault().getActiveShell(), context, scope, style, false, "");
			//dialog.
			dialog.setTitle("Add customized Item");
			dialog.setHelpAvailable(false);
			String message = MessageFormat.format("Add the customized Item names", new Object[] {element.getLocalName()});
			dialog.setMessage(message);
			Document doc = element.getOwnerDocument();
			Element absoluteOrdering = doc.createElementNS("http://www.testnamespace.com", "absolute-ordering");
			Node newChild1 = element.appendChild(absoluteOrdering);
			Object[] elements = {newChild1};
			dialog.setInitialSelections(elements);
			if (dialog.open() == ElementListSelectionDialog.OK) {
				Object result[] = dialog.getResult();
				for(int j = 0; j< result.length; j++)
				{
					if(result[0] instanceof Action) {
						Action action = (Action)result[0];
						action.run();
					}
				}
			
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
