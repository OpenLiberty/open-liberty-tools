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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.server.ui.wizard.WizardFragment;

public class AddonDropWizardFragment extends ExtendRuntimeWizardContainer {
    private List<WizardFragment> cachedList;

    @Override
    protected void createChildFragments(List<WizardFragment> list) {
        if (cachedList == null) {
            cachedList = new ArrayList<WizardFragment>();
            cachedList.add(new LicenseWizardFragment());
            cachedList.add(new InstallContentWizardFragment());
        }
        list.addAll(cachedList);
    }
}
