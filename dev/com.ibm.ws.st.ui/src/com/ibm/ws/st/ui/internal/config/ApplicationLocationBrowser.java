/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.xwt.dde.customization.IAdvancedCustomizationObject;

public class ApplicationLocationBrowser implements IAdvancedCustomizationObject {

    @Override
    public String invoke(String value, Node node, Element closestAncestor, IEditorPart editorPart) {
        Shell shell = editorPart.getSite().getShell();
        IEditorInput input = editorPart.getEditorInput();
        ApplicationLocationDialog dialog = new ApplicationLocationDialog(shell, closestAncestor.getOwnerDocument(), input);
        if (dialog.open() == Window.OK) {
            return dialog.getFullPath();
        }
        return null;
    }
}
