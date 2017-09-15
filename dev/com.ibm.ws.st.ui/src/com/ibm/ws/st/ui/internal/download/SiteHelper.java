/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.core.internal.repository.ISite;

public class SiteHelper {

    private static final String EXTENSION_ID = "com.ibm.ws.st.ui.siteManager";
    private static final String MANAGER_ELEMENT = "manager";
    private static final String CLASS_ATTRIBUTE = "class";

    private static AbstractSiteManager siteManager;

    private synchronized static AbstractSiteManager getSiteManager() {
        if (siteManager == null) {
            IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
            for (int i = 0; i < extensions.length; i++) {
                IConfigurationElement configurationElement = extensions[i];
                if (MANAGER_ELEMENT.equals(configurationElement.getName())) {
                    try {
                        Object object = configurationElement.createExecutableExtension(CLASS_ATTRIBUTE);
                        if (object instanceof AbstractSiteManager) {
                            siteManager = (AbstractSiteManager) object;
                            break;
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
            if (siteManager == null) {
                siteManager = new DefaultSiteManager();
            }
        }
        return siteManager;
    }

    public static boolean downloadAndInstallSupported() {
        return getSiteManager().downloadAndInstallSupported();
    }

    static ISite[] getPredefinedRuntimeSites() {
        return getSiteManager().getPredefinedRuntimeRepositories();
    }

    static ISite[] getAvailableSites() {
        return getSiteManager().getAvailableRepositories();
    }

    static SiteDelegate[] getConfigurableSites() {
        return getSiteManager().getConfigurableRepositories();
    }

    static SiteDelegate[] getSelectedSites() {
        return getSiteManager().getSelectedRepositories();
    }

    static ISite getDefaultAddOnSite() {
        return getSiteManager().getDefaultAddOnRepository();
    }

    static SiteDelegate getDefaultSiteDelegate() {
        return getSiteManager().getDefaultRepositoryDelegate();
    }

    static boolean isSocketAvailable(String host, int port) {
        if (host == null) {
            return false;
        }

        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(host, port), 10000);
        } catch (Throwable t) {
            return false;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                    // Do nothing.
                }
            }
        }

        return true;
    }

    public static boolean isProxyNeeded() {
        return getSiteManager().isProxyEnabled();
    }

    public static boolean isRepoSupported(ISite site, IRuntimeInfo runtimeInfo) {
        return getSiteManager().isRepoSupported(site, runtimeInfo);
    }

    public static boolean isZipRepoSupported(IRuntimeInfo runtimeInfo) {
        return getSiteManager().isZipRepoSupported(runtimeInfo);
    }

    public static boolean isValidOnPremZipRepository(File file) {
        return getSiteManager().isValidOnPremZipRepository(file);
    }

    public static String getRepoPropertiesURL() {
        return getSiteManager().getRepoPropertiesURL();
    }

    static boolean applyConfigSiteChanges(List<SiteDelegate> allSites, List<SiteDelegate> selectedSites) {
        return getSiteManager().applyConfigRepositoryChanges(allSites, selectedSites);
    }

    public static class SiteDelegate {
        private String name;
        private URL url;
        private String user;
        private String password;
        private boolean isDefault;
        private State state;

        public enum State {
            ORIGINAL, COPY, NEW
        }

        public SiteDelegate(String name) {
            this.name = name;
            this.state = State.ORIGINAL;
        }

        public SiteDelegate(SiteDelegate original) {
            this.name = original.name;
            this.url = original.url;
            this.user = original.user;
            this.password = original.password;
            this.state = State.COPY;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public void setIsDefault(boolean isDefault) {
            this.isDefault = isDefault;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public URL getURL() {
            return url;
        }

        public void setURL(URL url) {
            this.url = url;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }
    }
}
