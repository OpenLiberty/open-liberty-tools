/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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

public class DownloaderWizardFragment extends AbstractWizardFragment {

    @Override
    public Composite createComposite(Composite parent, IWizardHandle wizard) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP);
        IRuntimeHandler runtimeHandler = (IRuntimeHandler) getTaskModel().getObject(AbstractDownloadComposite.RUNTIME_HANDLER);
        String archiveSource = (String) getTaskModel().getObject(ARCHIVE_SOURCE);
        comp = new DownloaderComposite(parent, map, getContainer(wizard), getMessageHandler(wizard), runtimeHandler, archiveSource);
        return comp;
    }

    @Override
    public boolean isComplete() {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP);
        if (map.get(AbstractDownloadComposite.SELECTED_CORE_MANAGER) == null)
            return false;

        return super.isComplete();
    }
}
