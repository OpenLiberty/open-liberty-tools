/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package com.ibm.ws.st.core.tests;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.junit.Test;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Runtime test case base", isStable = true)
public abstract class RuntimeTestCaseBase extends TestCase {
    private static int NUMBER_OF_RUNTIMES = 0;

    protected static IRuntime[] runtime;
    protected static IRuntimeWorkingCopy[] runtimeWC;
    protected static WebSphereRuntime[] wsRuntime;

    private final String location;

    protected RuntimeTestCaseBase() {
        int numberOfRuntimes = getNumberOfRuntimes();
        assertTrue(numberOfRuntimes != 0);

        if (NUMBER_OF_RUNTIMES != numberOfRuntimes) {
            NUMBER_OF_RUNTIMES = numberOfRuntimes;
            runtime = new IRuntime[numberOfRuntimes];
            runtimeWC = new IRuntimeWorkingCopy[numberOfRuntimes];
            wsRuntime = new WebSphereRuntime[numberOfRuntimes];
        }

        location = WLPCommonUtil.getWLPInstallDir();
    }

    private static final PropertyChangeListener pcl = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent arg0) {
            // ignore
        }
    };

    @Test
    public void doSetup() throws Exception {
        WLPCommonUtil.print("Starting test: " + getClass().getName());
    }

    @Test
    public void testCreateRuntime() throws Exception {
        for (int i = 0; i < getNumberOfRuntimes(); ++i) {
            IRuntimeType rt = ServerCore.findRuntimeType(getRuntimeTypeId());
            IRuntimeWorkingCopy wc = rt.createRuntime(null, null);
            wc.setLocation(new Path(location));

            // Force a meta data creation and wait for the operation to finish.
            // This way we can be sure that it was created before we continue
            // with the rest of test.
            WebSphereRuntime wr = (WebSphereRuntime) wc.loadAdapter(WebSphereRuntime.class, null);
            TestUtil.createMetaData(wr);

            runtime[i] = wc.save(false, null);
            assertTrue(!runtime[i].isWorkingCopy());
        }
        TestUtil.jobWaitBuildandResource();
    }

    @Test
    public void testGetLocation() throws Exception {
        for (int i = 0; i < getNumberOfRuntimes(); ++i) {
            assertNotNull(runtime[i].getLocation());
        }
    }

    @Test
    public void testValidateRuntime() throws Exception {
        for (int i = 0; i < getNumberOfRuntimes(); ++i) {
            IStatus status = runtime[i].validate(null);
            assertTrue("Status of validation for runtime: " + runtime[i].getName() + " should be OK but is: " + status.getSeverity() + ", with message: " + status.getMessage(),
                       status.isOK());
        }
    }

    @Test
    public void testValidateRuntime2() throws Exception {
        for (int i = 0; i < getNumberOfRuntimes(); ++i) {
            IRuntimeWorkingCopy wc = runtime[i].createWorkingCopy();
            wc.setLocation(null);
            IStatus status = wc.validate(null);
            assertTrue(!status.isOK());
        }
    }

    @Test
    public void testAdaptRuntime() throws Exception {
        for (int i = 0; i < getNumberOfRuntimes(); ++i) {
            wsRuntime[i] = (WebSphereRuntime) runtime[i].loadAdapter(WebSphereRuntime.class, null);
            assertNotNull(wsRuntime[i]);
            assertNotNull(wsRuntime[i].getVMInstall());
            assertNotNull(wsRuntime[i].getConfigurationSchemaURL());
        }
    }

    @Test
    public void testModifyRuntime() throws Exception {
        for (int i = 0; i < getNumberOfRuntimes(); ++i) {
            IRuntimeWorkingCopy wc = runtime[i].createWorkingCopy();
            WebSphereRuntime trwc = (WebSphereRuntime) wc.loadAdapter(WebSphereRuntime.class, null);
            trwc.setVMInstall(null);
            wc.save(true, null);
            wsRuntime[i] = (WebSphereRuntime) runtime[i].loadAdapter(WebSphereRuntime.class, null);
            assertNotNull(wsRuntime[i].getVMInstall());
        }
        TestUtil.wait("Wait for runtime modify", 1000);
        TestUtil.jobWait(Constants.JOB_FAMILY);
    }

    @Test
    public void testDeleteRuntime() throws Exception {
        for (int i = 0; i < getNumberOfRuntimes(); ++i) {
            deleteRuntime(i);
        }
    }

    @Test
    public void doTearDown() {
        WLPCommonUtil.cleanUp();
        WLPCommonUtil.print("Ending test: " + getClass().getName() + "\n");
    }

    protected abstract int getNumberOfRuntimes();

    protected IRuntimeWorkingCopy getRuntimeWorkingCopy(int i) throws Exception {
        if (runtimeWC[i] == null)
            runtimeWC[i] = runtime[i].createWorkingCopy();

        return runtimeWC[i];
    }

    protected void createWorkingCopy(int i) throws Exception {
        getRuntimeWorkingCopy(i);
    }

    protected void isWorkingCopyDirty(int i) throws Exception {
        assertFalse(getRuntimeWorkingCopy(i).isDirty());
    }

    protected void addPropertyChangeListener(int i) throws Exception {
        getRuntimeWorkingCopy(i).addPropertyChangeListener(pcl);
    }

    protected void removePropertyChangeListener(int i) throws Exception {
        getRuntimeWorkingCopy(i).removePropertyChangeListener(pcl);
    }

    private void deleteRuntime(int i) throws Exception {
        runtime[i] = null;
        wsRuntime[i] = null;
        runtimeWC[i] = null;
    }

    protected String getRuntimeTypeId() {
        return WLPCommonUtil.RUNTIME_ID;
    }
}
