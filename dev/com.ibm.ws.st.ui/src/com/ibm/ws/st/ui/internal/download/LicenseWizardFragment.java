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

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.License;

/**
 * A composite used to accept a license.
 */
public class LicenseWizardFragment extends AbstractWizardFragment {

    @Override
    public Composite createComposite(Composite parent, IWizardHandle wizard) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP);
        comp = new LicenseComposite(parent, map, getContainer(wizard), getMessageHandler(wizard));
        return comp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean hasComposite() {
        Map<String, Object> map = (Map<String, Object>) getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP);
        if (map == null)
            return false;

        Map<IProduct, License> licenseMap = (Map<IProduct, License>) map.get(AbstractDownloadComposite.LICENSE);
        return licenseMap != null && !licenseMap.isEmpty();
    }

    @Override
    public boolean isComplete() {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP);
        if (!Boolean.TRUE.equals(map.get(AbstractDownloadComposite.LICENSE_ACCEPT)))
            return false;

        return super.isComplete();
    }
}
