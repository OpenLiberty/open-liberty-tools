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
package com.ibm.ws.st.ui.internal.actions;

import java.net.URI;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.DDETreeContentProvider;
import com.ibm.ws.st.ui.internal.Messages;

public class OpenSchemaBrowserAction extends SelectionProviderAction {
    private URI uri;

    public OpenSchemaBrowserAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.actionOpenConfigurationSchema);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.isEmpty()) {
            setEnabled(false);
            return;
        }
        boolean enabled = false;
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof ConfigurationFile) {
                ConfigurationFile configFile = (ConfigurationFile) obj;
                uri = configFile.getURI();
                enabled = true;
            } else if (obj instanceof Element) {
                Element element = (Element) obj;
                uri = DDETreeContentProvider.getURI(element);
                enabled = true;
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(enabled);
    }

    @Override
    public void run() {
        if (uri == null)
            return;

        Activator.openSchemaBrowser(uri);
    }
}
