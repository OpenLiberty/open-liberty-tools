/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.tests.docker.DockerTestUtil;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Check Utility", isStable = false)
@RunWith(AllTests.class)
public class DockerUtilitiesTest extends ToolsTestBase {
    protected static final String SERVER_NAME = "serverUtilsTestServer";
    protected static final Pattern SERVER_DUMP_FILE_NAME_PATTERN = Pattern.compile(SERVER_NAME + ".dump-\\d{2}\\.\\d{2}\\.\\d{2}\\_\\d{2}\\.\\d{2}\\.\\d{2}\\.zip");
    protected static final String JAVA_DUMP_IBM_FILE_NAME_PATTERN = "javacore..+.txt";
    protected static final String JAVA_DUMP_ORACLE_FILE_NAME_PATTERN = "javadump..+.txt";

    protected static final String RESOURCE_LOCATION = "UtilityTests";
    protected static final String SERVER_LOCATION = "resources/UtilityTests/" + SERVER_NAME;

    protected static WebSphereRuntime wsRuntime;
    protected static WebSphereServerInfo wsServerInfo;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(DockerUtilitiesTest.getOrderedTests());
        suite.setName(DockerUtilitiesTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(DockerUtilitiesTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(DockerUtilitiesTest.class, "testServerDump"));
        testSuite.addTest(TestSuite.createTest(DockerUtilitiesTest.class, "testJavaDump"));
        testSuite.addTest(TestSuite.createTest(DockerUtilitiesTest.class, "testSSLCertificate"));
        testSuite.addTest(TestSuite.createTest(DockerUtilitiesTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: UtilitiesTestCase");
        init();
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER_NAME, SERVER_LOCATION);
        createVM(JDK_NAME);
        wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        wsServerInfo = wsServer.getServerInfo();

        assertNotNull(wsServerInfo);
        assertNotNull(wsServerInfo.getUserDirectory());
        wait("wait 3 seconds before packaging server.", 3000);
    }

    @Test
    public void testServerDump() {
        assertNotNull("Server is null. Cannot runt the test.", wsServer);

        File dumpFile = wsServer.getServerInfo().getServerOutputPath().append(wsServer.getServerName() + "_" + "dump").addFileExtension("zip").toFile();
        if (dumpFile.exists())
            assertTrue("Could not delete prior server dump: " + dumpFile.getAbsolutePath(), FileUtil.safeDelete(dumpFile));

        try {
            WebSphereRuntime wsRuntime = getWebSphereRuntime();
            assertNotNull("Websphere Runtime is null, cannot run the test", wsRuntime);
            ILaunch launch = wsRuntime.dumpServer(wsServer.getServerInfo(), dumpFile, "heap", null);
            assertTrue("Could not generate dump files for server ", dumpFile.exists() && dumpFile.length() > 1024);
            print("Dump file from remote server was downloaded to location: " + dumpFile.getAbsolutePath());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (dumpFile.exists())
                assertTrue("Could not delete server dump: " + dumpFile.getAbsolutePath(), FileUtil.safeDelete(dumpFile));
        }
    }

    @Test
    public void testJavaDump() throws Exception {

        assertNotNull("Server is null. Cannot runt the test.", wsServer);
        String javaPlatform = getJMXMBeanAttributeValue("java.lang:type=Runtime", "SystemProperties", "java.vendor");
        assertNotNull("Error getting the java vendor information for remote server", javaPlatform);
        print("Server java vendor: " + javaPlatform);

        String dumpFilePattern;

        if (javaPlatform != null && javaPlatform.contains("IBM"))
            dumpFilePattern = JAVA_DUMP_IBM_FILE_NAME_PATTERN;
        else
            dumpFilePattern = JAVA_DUMP_ORACLE_FILE_NAME_PATTERN;

        ArrayList<String> dumpFiles = getJavaDumpFiles(dumpFilePattern);
        if (dumpFiles.size() > 0) {
            for (String dumpFile : dumpFiles) {
                print("Deleting existing dumpfile at: " + dumpFile);
                jmxConnection.deleteFile(dumpFile);
            }
        }

        try {
            WebSphereRuntime wsRuntime = getWebSphereRuntime();
            assertNotNull("Websphere Runtime is null, cannot run the test", wsRuntime);

            wsRuntime.javadumpServer(wsServer.getServerInfo(), null, null);
            // java dump can fail when running on JRE instead of JDK

            dumpFiles = getJavaDumpFiles(dumpFilePattern);
            assertTrue("Did not find any dump files", dumpFiles != null && dumpFiles.size() > 0);
        } catch (TimeoutException e) {
            Assert.fail("Java dump did not complete in " + LAUNCH_TIMEOUT + " seconds");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    protected ArrayList<String> getJavaDumpFiles(final String pattern) throws Exception {
        CompositeData[] listOfServerDirFiles = jmxConnection.getDirectoryEntries("${server.output.dir}", false, "a");
        ArrayList<String> fileNames = new ArrayList<String>();
        if (listOfServerDirFiles != null) {
            for (CompositeData metaData : listOfServerDirFiles) {
                boolean isDir = (Boolean) metaData.get("directory");
                if (!isDir) {
                    String filePath = (String) metaData.get("fileName");
                    String[] list = filePath.split("/");
                    String fileName = list[list.length - 1];
                    if (fileName != null && fileName.matches(pattern)) {
                        fileNames.add(filePath);
                    }
                }
            }
        }
        return fileNames;
    }

    @Test
    public void testSSLCertificate() {
        assertNotNull("Server is null. Cannot runt the test.", wsServer);

        String includeName = Constants.GENERATED_SSL_INCLUDE;
        String password = "sslTest";
        File generatedSSLIncludeFile = wsServer.getServerInfo().getServerOutputPath().append(includeName).toFile();
        boolean includeExists = false;
        long timeStamp = 0;
        if (generatedSSLIncludeFile.exists()) {
            includeExists = true;
            timeStamp = generatedSSLIncludeFile.lastModified();
            print("Server has an existing " + includeName + " with timestamp:" + timeStamp);
        }

        try {
            WebSphereRuntime wsRuntime = getWebSphereRuntime();
            assertNotNull("Websphere Runtime is null, cannot run the test", wsRuntime);

            String containerName = DockerTestUtil.getContainerName(server);
            assertNotNull("The container name for the " + server.getName() + " server should not be null.", containerName);
            BaseDockerContainer container = DockerTestUtil.getExistingContainer(DockerTestUtil.getDockerMachine(), containerName);
            String remoteOutputPath = wsServer.getServerInfo().getUserDirectory().getRemoteUserPath().toString().replace("usr", "output");
            IPath keystoreName = new Path(remoteOutputPath + "/" + wsServer.getServerInfo().getServerName()).append("/resources/security/key.p12");

            if (container.fileExists(keystoreName.toOSString())) {
                print("Keystore exists, delete keystore for testing the utility of generating keystore");
                container.deleteFile(keystoreName.toOSString());
            }

            wsRuntime.createSSLCertificate(wsServer.getServerInfo(), password, "xor", null, -1, null, includeName, null);

            // if runtime is 8.5.5.2 or higher then it supports generating include files instead of having to copy and paste config into server.xml
            if (WebSphereUtil.isGreaterOrEqualVersion("8.5.5.2", wsRuntime.getRuntimeVersion())) {
                assertTrue("Could not find SSL include file in " + generatedSSLIncludeFile.getAbsolutePath(), generatedSSLIncludeFile.exists());
            }

            //test if file is included in the server.xml only if the file didn't exist already
            if (!includeExists)
                assertTrue("The " + includeName + " file was not automatically included in server config file", configFileHasInclude(includeName));
            else
                print("New timestamp on " + includeName + ":" + generatedSSLIncludeFile.lastModified());

            assertTrue("Timestamps on the old " + includeName + " file and new file are same, whereas a new file should have a new timestamp",
                       timeStamp != generatedSSLIncludeFile.lastModified());
            assertTrue("Couldn't connect to the server", jmxConnection.isConnected());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        wait("Exiting test: DockerUtilitiesTest", 1000);
    }
}