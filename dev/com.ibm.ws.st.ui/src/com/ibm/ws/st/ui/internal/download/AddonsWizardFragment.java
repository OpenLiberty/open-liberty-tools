/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;

public class AddonsWizardFragment extends AbstractWizardFragment {

    @Override
    public Composite createComposite(Composite parent, IWizardHandle wizard) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP);
        comp = new AddonsComposite(parent, map, getContainer(wizard), getMessageHandler(wizard));
        return comp;
    }

    @Override
    public boolean hasComposite() {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP);
        if (map == null)
            return false;

        IRuntimeInfo core = (IRuntimeInfo) map.get(AbstractDownloadComposite.RUNTIME_CORE);
        if (core == null)
            return false;

        if (core.getProductVersion() == null || core.getProductVersion().startsWith("8.5.0")) {
            return false;
        }

        IProduct coreManager = (IProduct) map.get(AbstractDownloadComposite.SELECTED_CORE_MANAGER);
        if (coreManager instanceof LocalProduct) {
            String archivePath = coreManager.getSource().getLocation();
            if (!WebSphereRuntime.supportsInstallingAdditionalContent(archivePath)) {
                return false;
            }
        }

        return true;
    }
}
