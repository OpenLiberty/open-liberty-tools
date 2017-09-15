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
package com.ibm.ws.st.ui.internal.download;

import java.io.File;
import java.util.List;

import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.core.internal.repository.ISite;
import com.ibm.ws.st.ui.internal.download.SiteHelper.SiteDelegate;

/**
 * Abstract site manager class.
 */
public abstract class AbstractSiteManager {

    public boolean downloadAndInstallSupported() {
        return false;
    }

    public ISite[] getPredefinedRuntimeRepositories() {
        return new ISite[0];
    }

    public ISite[] getAvailableRepositories() {
        return new ISite[0];
    }

    public SiteDelegate[] getConfigurableRepositories() {
        return new SiteDelegate[0];
    }

    public SiteDelegate[] getSelectedRepositories() {
        return new SiteDelegate[0];
    }

    public ISite getDefaultAddOnRepository() {
        return null;
    }

    public SiteDelegate getDefaultRepositoryDelegate() {
        return null;
    }

    public boolean isProxyEnabled() {
        return false;
    }

    public boolean isRepoSupported(ISite site, IRuntimeInfo runtimeInfo) {
        return false;
    }

    public boolean isZipRepoSupported(IRuntimeInfo runtimeInfo) {
        return false;
    }

    public boolean isValidOnPremZipRepository(File file) {
        return false;
    }

    public String getRepoPropertiesURL() {
        return null;
    }

    public boolean applyConfigRepositoryChanges(List<SiteDelegate> allSites, List<SiteDelegate> selectedSites) {
        return false;
    }

}
