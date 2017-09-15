/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.remote;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServer.IOperationListener;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test remote server RXA disabled", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RemoteServerStartDisabledTest extends ToolsTestBase {

    // Do not change the runtime name as the application projects are targeted to this runtime
    protected static final String RUNTIME_NAME = "remoteServerTestRuntime";

    @Test
    public void test01_doSetup() throws Exception {
        doRemoteSetup(RUNTIME_NAME);
    }

    @Test
    public void test02_createServer() throws Exception {
        createRemoteServer(runtime);

        // It turns out that Server.restart will do nothing and return OK status
        // if the server is in stopped state and it can take a bit of time for
        // the state of a newly created remote server to get updated so wait here
        // for the state to be started.
        for (int i = 0; i < 30 && server.getServerState() != IServer.STATE_STARTED; i++) {
            print("Waiting for server started state: " + i);
            Thread.sleep(1000);
        }
        assertTrue("The server should be in started state.  The actual state is: " + server.getServerState(), server.getServerState() == IServer.STATE_STARTED);
    }

    @Test
    public void test03_attemptRestart() throws Exception {

        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, false);
        server = wc.save(true, null);

        final IServer serverf = server;
        final IStatus[] status = { null };

        serverf.restart(ILaunchManager.RUN_MODE, new IOperationListener() {

            @Override
            public void done(IStatus stat) {
                status[0] = stat;
            }

        });
        int timeout = 0;
        while (status[0] == null && timeout < 100) {
            Thread.sleep(1000);
            timeout++;
        }
        assertNotNull("Status is still null, server restart didn't complete normally", status[0]);
        assertTrue("The status should have error severity.  Expected: " + IStatus.ERROR + ", Actual: " + status[0].getSeverity(), status[0].getSeverity() == IStatus.ERROR);
        String expectedMsg = com.ibm.ws.st.core.internal.Messages.errorPromptRemoteServerSettingsDisabled;
        if (com.ibm.ws.st.common.core.ext.internal.Activator.getPlatformProvider(PlatformType.SSH_KEYLESS.name()) == null)
            expectedMsg = com.ibm.ws.st.core.internal.Messages.errorPromptRemoteServerActionsUnavailable;
        assertTrue("Status error message is not correct.  Expected:  " + expectedMsg + ",  Actual: " + status[0].getMessage(),
                   status[0].getMessage().equals(expectedMsg));
    }

    @Test
    public void test99_doTearDown() {
        cleanUp();
    }

}
