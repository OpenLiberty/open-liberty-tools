/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.common.ui.ext.internal.servertype.ServerTypeUIExtension;

public class WebSphereServerWizardContainer extends WebSphereServerWizardCommonFragment {
    private List<WizardFragment> cachedList;
    static final String RUNTIME_HAS_SERVERS = "hasServers";

    /**
     *
     */
    public WebSphereServerWizardContainer() {
        // do nothing
    }

    @Override
    protected void createChildFragments(List<WizardFragment> list) {
        if (cachedList == null) {
            cachedList = new ArrayList<WizardFragment>();
            cachedList.add(new WebSphereServerWizardFragment());
            cachedList.add(new WebSphereRemoteServerWizardFragment());
            for (ServerTypeUIExtension ext : getServerTypeExtensions()) {
                cachedList.addAll(ext.getFollowingFragments());
            }
        }
        list.addAll(cachedList);
    }
}
