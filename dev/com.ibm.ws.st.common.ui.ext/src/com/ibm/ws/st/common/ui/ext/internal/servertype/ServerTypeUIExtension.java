/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.ui.ext.internal.servertype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.common.ui.ext.internal.Trace;

/**
 * Represents a server type UI extension and provides methods
 * to retrieve fields from the extension point.
 */
public class ServerTypeUIExtension {

    public static final String LIBERTY_SERVER = "libertyServer";
    private static final String ID_ATTR = "id";
    private static final String TYPE_ID_ATTR = "typeId";
    private static final String TYPE_LABEL_ATTR = "typeLabel";
    private static final String MNEMONIC_ATTR = "mnemonic";

    private static ServerTypeUIExtension[] serverTypeUIExtensions = null;

    IConfigurationElement extensionElem;
    WizardFragmentProvider fragProvider = null;

    public static ServerTypeUIExtension[] getServerTypeUIExtensions() {
        if (serverTypeUIExtensions != null)
            return serverTypeUIExtensions;

        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor("com.ibm.ws.st.common.ui.ext.serverTypeUIExtension");
        List<ServerTypeUIExtension> extList = new ArrayList<ServerTypeUIExtension>();

        for (IConfigurationElement elem : configElements) {
            extList.add(new ServerTypeUIExtension(elem));
        }

        serverTypeUIExtensions = extList.toArray(new ServerTypeUIExtension[extList.size()]);
        return serverTypeUIExtensions;
    }

    public ServerTypeUIExtension(IConfigurationElement extensionElem) {
        this.extensionElem = extensionElem;
    }

    public String getId() {
        return extensionElem.getAttribute(ID_ATTR);
    }

    public String getTypeId() {
        return extensionElem.getAttribute(TYPE_ID_ATTR);
    }

    public String getLabel() {
        return updateMnemonic(extensionElem.getAttribute(TYPE_LABEL_ATTR));
    }

    public boolean isActive(TaskModel taskModel) {
        WizardFragmentProvider provider = getFragProvider();
        if (provider != null) {
            return provider.isActive(taskModel);
        }
        return false;
    }

    public Composite getComposite(Composite parent, IWizardHandle handle, TaskModel taskModel) {
        WizardFragmentProvider provider = getFragProvider();
        if (provider != null) {
            return provider.getInitialComposite(parent, handle, taskModel);
        }
        return null;
    }

    public List<WizardFragment> getFollowingFragments() {
        WizardFragmentProvider provider = getFragProvider();
        if (provider != null) {
            return provider.getFollowingFragments();
        }
        return Collections.emptyList();
    }

    private WizardFragmentProvider getFragProvider() {
        if (fragProvider == null) {
            try {
                fragProvider = (WizardFragmentProvider) extensionElem.createExecutableExtension("serverWizardClass");
            } catch (CoreException e) {
                Trace.logError("Error while creating server type UI extension for " + getId(), e);
            }
        }
        return fragProvider;
    }

    //method from org.eclipse.ui.workbench/org.eclipse.ui.menus.CommandContributionItem
    private String updateMnemonic(String s) {
        String mnemonic = extensionElem.getAttribute(MNEMONIC_ATTR);
        if (mnemonic == null || s == null) {
            return s;
        }
        int idx = s.indexOf(mnemonic);
        if (idx == -1) {
            return s;
        }

        return s.substring(0, idx) + '&' + s.substring(idx);
    }

}
