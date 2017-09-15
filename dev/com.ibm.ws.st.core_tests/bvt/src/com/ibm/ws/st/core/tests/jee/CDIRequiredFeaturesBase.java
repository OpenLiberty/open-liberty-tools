/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.module.ModuleHelper;

/**
 * Base test class for all CDI required features tests
 */
@RunWith(AllTests.class)
public class CDIRequiredFeaturesBase extends JEETestBase {
    protected static final String SERVER12_LOCATION = "resources/jee/cdi/cdi12Server";
    protected static final String SERVER12_NAME = "cdi12Server";
    protected static final String ROOT_RESOURCE_PATH = "jee/cdi";
    protected static final String PROJECT_NAME = "WebProject";

    protected void addApp() throws Exception {
        IModule module = ModuleHelper.getModule(PROJECT_NAME);
        IServerWorkingCopy wc = server.createWorkingCopy();
        IModule[] modules = new IModule[] { module };
        wc.modifyModules(modules, null, null);
        wc.save(true, null);
    }

}