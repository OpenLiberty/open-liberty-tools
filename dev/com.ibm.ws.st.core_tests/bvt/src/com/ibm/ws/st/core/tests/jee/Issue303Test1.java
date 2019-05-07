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

import org.eclipse.core.runtime.Path;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test publish works when a utility project added to an ear depends on an external jar", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Issue303Test1 extends JEETestBase {

    @Override
    protected boolean isLooseCfg() {
        return true;
    }

    @Test
    public void test01_doSetup() throws Exception {
        print("Starting test: Issue303Test1");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("jee/Issue303Test1"), new String[] {
                                                                     "ResourceProject",
                                                                     "UtilProject",
                                                                     "WebProject",
                                                                     "WebProjectEAR" });
        startServer();
    }

    @Test
    public void test02_testServlet1() throws Exception {
        wait("wait 3 seconds before adding application.", 3000);
        addApp("WebProjectEAR", false, 30);
        wait("Wait 5 seconds before ping", 5000);
        testPingWebPage("WebProject/TestServlet", "Hello from TestServlet Util.getInfo() Util2.getInfo");
        removeApp("WebProjectEAR", 2500);
    }

    @Test
    public void test99_doTearDown() throws Exception {
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: Issue303Test1\n", 5000);
    }
}