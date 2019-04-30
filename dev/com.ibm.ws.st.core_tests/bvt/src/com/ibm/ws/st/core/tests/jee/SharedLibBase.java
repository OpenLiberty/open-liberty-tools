/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.Application;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.LibRef;
import com.ibm.ws.st.core.internal.config.validation.AbstractConfigurationValidator;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.jee.core.internal.JEEServerExtConstants;
import com.ibm.ws.st.jee.core.internal.SharedLibertyUtils;

// Common code for shared lib tests
public abstract class SharedLibBase extends ToolsTestBase {

    protected static final String SERVER_NAME = "SharedLibTest2";
    protected static final String WAR_NAME = "Web1";
    protected static final String EAR_NAME = "Web1EAR";
    protected static final String UTIL1_NAME = "Util1";
    protected static final String UTIL2_NAME = "Util2";
    protected static final String UTIL3_NAME = "Util3";
    protected static final String UTIL1_ID = "util1";
    protected static final String UTIL2_ID = "util2";
    protected static final String UTIL3_ID = "util3";
    protected static final String MARKER_TYPE = "com.ibm.ws.st.core.configmarker";

    protected Application getApp(String name) {
        final ConfigurationFile configFile = wsServer.getConfiguration();
        Application[] apps = configFile.getApplications();
        for (Application app : apps) {
            if (app.getName().equals(name)) {
                return app;
            }
        }
        return null;
    }

    protected void setupUtilLibrary(IProject project, String id) {
        IPath libPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("tmp");
        Properties props = new Properties();
        props.setProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_ID_KEY, id);
        props.setProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_DIR_KEY, libPath.toOSString());
        IPath path = project.getLocation().append(JEEServerExtConstants.SHARED_LIBRARY_SETTING_FILE_PATH);
        SharedLibertyUtils.saveSettings(props, path);
    }

    protected void runQuickFix(String appName) throws Exception {
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
        WLPCommonUtil.jobWaitBuild();

        IMarker[] markers = ResourcesPlugin.getWorkspace().getRoot().findMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        IMarker quickFixMarker = null;
        if (markers != null) {
            for (IMarker m : markers) {
                int fixType = m.getAttribute(AbstractConfigurationValidator.QUICK_FIX_TYPE_ATTR,
                                             AbstractConfigurationValidator.QuickFixType.NONE.ordinal());

                if (fixType == AbstractConfigurationValidator.QuickFixType.OUT_OF_SYNC_SHARED_LIB_REF_MISMATCH.ordinal()) {
                    final String name = m.getAttribute(AbstractConfigurationValidator.APPLICATION_NAME, "");
                    if (name.equals(appName)) {
                        quickFixMarker = m;
                        break;
                    }
                }
            }
        }

        assertNotNull("Could not find out of sync marker", quickFixMarker);
        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(quickFixMarker);
        assertTrue("Did not get any marker resolutions.", resolutions.length == 1);
        resolutions[0].run(quickFixMarker);
        wait("Waiting for quick fix", 3000);
    }

    protected void checkLibRefs(Application app, List<LibRef> expectedRefs) throws Exception {
        List<LibRef> refs = app.getSharedLibRefs();
        assertTrue("The length of the library references should be: " + expectedRefs.size() + ", but it is: " + refs.size(), refs.size() == expectedRefs.size());
        for (LibRef expectedRef : expectedRefs) {
            int index = LibRef.getListIndex(refs, expectedRef.id);
            assertTrue("The library ref should exist for id: " + expectedRef.id, index >= 0);
            LibRef ref = refs.get(index);
            assertTrue("The library ref should have type: " + expectedRef.type + ", but it has type: " + ref.type, ref.type == expectedRef.type);
        }
    }
}
