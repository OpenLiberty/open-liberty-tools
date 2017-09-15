/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class TestsPlugin extends Plugin {
    private static TestsPlugin instance;
    private Path installLocationPath;
    public static final String PLUGIN_ID = "com.ibm.ws.st.core_tests";

    public TestsPlugin() {
        super();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static TestsPlugin getInstance() {
        return instance;
    }

    public static IPath getInstallLocation() {
        TestsPlugin curPlugin = getInstance();
        if (curPlugin.installLocationPath == null) {
            String installLocation = getBundleFullLocationPath(getInstance().getBundle());
            curPlugin.installLocationPath = new Path(installLocation);
        }
        return curPlugin.installLocationPath;
    }

    public static String getBundleFullLocationPath(Bundle curBundle) {
        if (curBundle == null) {
            return null;
        }
        URL installURL = curBundle.getEntry("/");
        String installLocation = null;
        try {
            URL realURL = FileLocator.resolve(installURL);
            installLocation = realURL.getFile();

            // Drop the beginning and end /
            if (installLocation != null && installLocation.startsWith("/") && installLocation.indexOf(":") > 0) {
                installLocation = installLocation.substring(1);
                // Make sure the path ends with a '/'		
                if (!installLocation.endsWith("/")) {
                    installLocation = installLocation + "/";
                }
            }
        } catch (IOException e) {
            System.out.println("Could not get the Plugin Full location Path:" + " getPluginFullLocationPath()");
        }
        return installLocation;
    }
}