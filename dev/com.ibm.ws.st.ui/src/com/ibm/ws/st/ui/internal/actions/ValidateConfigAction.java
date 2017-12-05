/*
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 */
package com.ibm.ws.st.ui.internal.actions;

import java.util.Iterator;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.validation.internal.ui.ValidationMenuAction;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * This class will work with the server.xml in the Servers view through
 * <code>com.ibm.ws.st.ui.internal.actions.WebSphereActionProvider</code>
 */
@SuppressWarnings("restriction")
public class ValidateConfigAction extends SelectionProviderAction {
    private final ValidationMenuAction delegate;

    protected ValidateConfigAction(ISelectionProvider provider) {
        super(provider, org.eclipse.wst.validation.internal.ui.ValidationUIMessages.Validate);
        delegate = new ValidationMenuAction();
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel == null || sel.size() != 1) {
            setEnabled(false);
            return;
        }

        ISelection configFileSelection = null;
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            // The server.xml in the Servers view is a DOM Element object.
            if (obj instanceof Element) {
                Element element = (Element) obj;
                try {
                    // Excludes children of the server.xml
                    if (Constants.SERVER_ELEMENT.equals(element.getTagName())) {
                        WebSphereServerInfo wsi = Platform.getAdapterManager().getAdapter(obj, WebSphereServerInfo.class);

                        // Notify the Action delegate that the file to be Validated has changed
                        // The delegate only accepts ISelections, and the object contained in the selection
                        // must be an IResource.
                        configFileSelection = new StructuredSelection(new Object[] { wsi.getConfigRoot().getIFile() });
                        delegate.selectionChanged(this, configFileSelection);
                    }
                } catch (Exception e) {
                    Trace.logError(getClass().getSimpleName() + ".updateSelection", e);
                    return;
                }
            }
        }
        setEnabled(configFileSelection != null);
    }

    @Override
    public void run() {
        delegate.run(this);
    }
}