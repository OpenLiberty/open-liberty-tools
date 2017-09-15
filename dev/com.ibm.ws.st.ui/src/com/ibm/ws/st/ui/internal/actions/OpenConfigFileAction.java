/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.DDETreeContentProvider;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.utility.PathUtil;

public class OpenConfigFileAction extends SelectionProviderAction {
    private URI uri;
    private IFile file;
    private String xpath;
    private boolean extended;

    public OpenConfigFileAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.actionOpenConfiguration);
        setActionDefinitionId("org.eclipse.jdt.ui.edit.text.java.open.editor");
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }

        file = null;
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof Element) {
                Element element = (Element) obj;
                extended = false;
                xpath = DOMUtils.createXPath(element);
                uri = DDETreeContentProvider.getURI(element);
                file = PathUtil.getBestIFileMatchForURI(uri);
                if (file == null) {
                    WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
                    for (WebSphereServerInfo server : servers) {
                        ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
                        if (configFile != null) {
                            file = configFile.getIFile();
                            break;
                        }
                    }
                }
            } else if (obj instanceof ConfigurationFile) {
                ConfigurationFile configFile = (ConfigurationFile) obj;
                extended = false;
                uri = configFile.getURI();
                file = configFile.getIFile();
            } else if (obj instanceof ExtendedConfigFile) {
                ExtendedConfigFile configFile = (ExtendedConfigFile) obj;
                extended = true;
                uri = configFile.getURI();
                file = PathUtil.getBestIFileMatchForURI(uri);
                if (file == null) {
                    file = configFile.getIFile();
                }
            } else if (obj instanceof IFile) {
                file = (IFile) obj;
                uri = file.getLocation().toFile().toURI();
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(uri != null);
    }

    @Override
    public void run() {
        if (uri == null)
            return;

        if (extended)
            Activator.openEditor(file, uri);
        else {
            Activator.openConfigurationEditor(file, uri, xpath);
        }
    }

}
