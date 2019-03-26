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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.wst.server.core.IServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;
import com.ibm.ws.st.ui.internal.download.DownloadHelper;

import junit.framework.AssertionFailedError;
import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Check Utility", isStable = false)
@RunWith(AllTests.class)
public class UtilitiesTestCase extends ToolsTestBase {
    protected static final String SERVER_NAME = "serverUtilsTestServer";
    protected static final Pattern SERVER_DUMP_FILE_NAME_PATTERN = Pattern.compile(SERVER_NAME + ".dump-\\d{2}\\.\\d{2}\\.\\d{2}\\_\\d{2}\\.\\d{2}\\.\\d{2}\\.zip");
    protected static final Pattern JAVA_DUMP_IBM_FILE_NAME_PATTERN = Pattern.compile("javacore..+.txt");
    protected static final Pattern JAVA_DUMP_ORACLE_FILE_NAME_PATTERN = Pattern.compile("javadump..+.txt");
    protected static final String RESOURCE_LOCATION = "UtilityTests";
    protected static final String SERVER_LOCATION = "resources/UtilityTests/" + SERVER_NAME;

    protected static WebSphereRuntime wsRuntime;
    protected static WebSphereServerInfo wsServerInfo;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(UtilitiesTestCase.getOrderedTests());
        suite.setName(UtilitiesTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testSSLCertificate"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testPluginConfig"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testPackageUtilityUsr"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testPackageUtilityAll"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testPackageUtilityMinify"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testServerDump"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testJavaDump"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testInstallPackageAllZip"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testInstallPackageMinifyZip"));
        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "testInstallUsrZip"));

        testSuite.addTest(TestSuite.createTest(UtilitiesTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: UtilitiesTestCase");
        init();
        createRuntime(RUNTIME_NAME);

        // The server that is copied over already has a deployed application (noisyservlet)
        createServer(runtime, SERVER_NAME, SERVER_LOCATION);

        createVM(JDK_NAME);
        wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        wsServerInfo = wsServer.getServerInfo();

        assertNotNull(wsServerInfo);
        assertNotNull(wsServerInfo.getUserDirectory());
        wait("wait 3 seconds before packaging server.", 3000);
    }

    @Test
    public void testPackageUtilityAll() {
        File zipFile = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("packageTestAll.zip").toFile();

        if (zipFile.exists())
            Assert.assertTrue("Could not delete prior test package: " + zipFile.getAbsolutePath(), FileUtil.safeDelete(zipFile));

        try {
            stopServer();
            assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
            wsRuntime.packageServer(wsServerInfo, zipFile, "all", false, null);
            print("Waiting for package job to finish");
            boolean jobDone = TestUtil.jobWaitBuildandResource();
            Assert.assertTrue("Did not wait for package job", jobDone);

            Assert.assertTrue("Package was not created: " + zipFile.getAbsolutePath(), zipFile.exists());

            waitForZipFile(zipFile, 1024, 10);
            Assert.assertTrue("Package suspiciously small (" + zipFile.length() + ") " + zipFile.getAbsolutePath(), zipFile.length() > 1024);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPackageUtilityUsr() {
        File zipFile = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("packageTestUsr.zip").toFile();

        if (zipFile.exists())
            Assert.assertTrue("Could not delete prior test package: " + zipFile.getAbsolutePath(), FileUtil.safeDelete(zipFile));

        try {
            startServer();
            assertTrue("Server should be started", testServerState(IServer.STATE_STARTED));
            wsRuntime.packageServer(wsServerInfo, zipFile, "usr", false, null);
        } catch (IllegalArgumentException e) {
            assertEquals(Messages.serverMustBeStopped, e.getMessage());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            stopServer();
            assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
            wsRuntime.packageServer(wsServerInfo, zipFile, "usr", false, null);

            print("Waiting for package job to finish");
            boolean jobDone = TestUtil.jobWaitBuildandResource();
            Assert.assertTrue("Did not wait for package job", jobDone);

            Assert.assertTrue("Package was not created: " + zipFile.getAbsolutePath(), zipFile.exists());

            waitForZipFile(zipFile, 1024, 10);
            Assert.assertTrue("Package suspiciously small (" + zipFile.length() + ") " + zipFile.getAbsolutePath(), zipFile.length() > 1024);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPackageUtilityMinify() {
        File zipFile = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("packageTestMinify.zip").toFile();

        if (zipFile.exists())
            Assert.assertTrue("Could not delete prior test package: " + zipFile.getAbsolutePath(), FileUtil.safeDelete(zipFile));

        try {
            stopServer();
            assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
            wsRuntime.packageServer(wsServerInfo, zipFile, "minify", false, null);

            print("Waiting for package job to finish");
            boolean jobDone = TestUtil.jobWait(Constants.JOB_FAMILY);

            wait("wait 10 seconds after packaging server.", 10000);

            Assert.assertTrue("Did not wait for package job", jobDone);

            Assert.assertTrue("Package was not created: " + zipFile.getAbsolutePath(), zipFile.exists());

            waitForZipFile(zipFile, 1024, 20);
            Assert.assertTrue("Package suspiciously small (" + zipFile.length() + ") " + zipFile.getAbsolutePath(), zipFile.length() > 1024);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            // Wait for server to be in stopped state - the minify command starts the server briefly and
            // the ConsoleMonitorThread may pick this up.  If it does, it needs time to detect that
            // the server is stopped again.
            assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
        }
    }

    @Test
    public void testSSLCertificate() throws Exception {
        String includeName = "sslinclude.xml";
        File file = wsServerInfo.getServerPath().append("resources/security/key.p12").toFile();
        File includeFile = wsServerInfo.getServerOutputPath().append(includeName).toFile();
        IPath serverConfig = wsServerInfo.getServerOutputPath().append("server.xml");

        if (file.exists())
            Assert.assertTrue("Could not delete prior SSL certificate: " + file.getAbsolutePath(), FileUtil.safeDelete(file));

        try {
            ILaunch launch = wsRuntime.createSSLCertificate(wsServerInfo, "password", "xor", null, -1, null, includeName, null);
            int exitValue = waitForLaunch(launch);

            Assert.assertTrue("SSL certificate utility did not exit normally", exitValue == 0);

            Assert.assertTrue("SSL certificate was not created: " + file.getAbsolutePath(), file.exists());

            Assert.assertTrue("SSL certificate suspiciously small (" + file.length() + ") " + file.getAbsolutePath(), file.length() > 256);

            // if runtime is 8.5.5.2 or higher then it supports generating include files instead of having to copy and paste config into server.xml
            if (WebSphereUtil.isGreaterOrEqualVersion("8.5.5.2", wsRuntime.getRuntimeVersion())) {
                Assert.assertTrue("Could not find SSL include file in " + includeFile.getAbsolutePath(), includeFile.exists());
            }

            //add include element to server config
            Document config = FileUtil.getDOM(serverConfig.toOSString());
            Element element = DOMUtils.getElement(config, "/server");
            Element include = config.createElement("include");
            include.setAttribute("location", includeFile.getPath());
            element.appendChild(include);
            FileUtil.saveDOM(config, serverConfig.toOSString());

            //change keystore password
            Document dom = FileUtil.getDOM(includeFile.getPath());
            element = DOMUtils.getElement(dom, "/server/keyStore");
            element.setAttribute("password", "{xor}123123");
            FileUtil.saveDOM(dom, includeFile.getPath());

            //refresh resource
            boolean refreshed = TestUtil.refreshResource(wsServer.getFolder().getFile("server.xml"));
            assertTrue(refreshed);

            refreshed = TestUtil.refreshResource(wsServer.getFolder().getFile(includeName));
            assertTrue(refreshed);

            //start the server and listent to console output
            startServer();
            assertTrue("Server should be started", testServerState(IServer.STATE_STARTED));
            String log = readConsole();
            assertTrue("Keystore was tampered with, or password was incorrect not found", log.contains("CWPKI0033E"));

        } catch (TimeoutException e) {
            Assert.fail("SSL certificate did not complete in " + LAUNCH_TIMEOUT + " seconds");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            stopServer();
            assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
        }
    }

    @Test
    public void testServerDump() throws Exception {
        startServer();
        assertTrue("Server should be started", testServerState(IServer.STATE_STARTED));

        File dumpFolder = wsServerInfo.getServerOutputPath().toFile();
        File[] dumpFiles = getFiles(dumpFolder, SERVER_DUMP_FILE_NAME_PATTERN);
        if (dumpFiles != null) {
            for (File dumpFile : dumpFiles) {
                Assert.assertTrue("Could not delete prior server dump: " + dumpFile.getAbsolutePath(), FileUtil.safeDelete(dumpFile));
            }
        }

        File zipFile = null;
        try {
            ILaunch launch = wsRuntime.dumpServer(wsServerInfo, null, null, null);
            int exitValue = waitForLaunch(launch);

            Assert.assertTrue("Server dump did not exit normally.  Exit value: " + exitValue, exitValue == 0);

            dumpFiles = getFiles(dumpFolder, SERVER_DUMP_FILE_NAME_PATTERN);
            Assert.assertTrue("Could not generate dump files for server " + SERVER_NAME, dumpFiles != null && dumpFiles.length > 0);
            zipFile = dumpFiles[0];
            Assert.assertTrue("Server dump suspiciously small (" + zipFile.length() + ") " + zipFile.getAbsolutePath(), zipFile.length() > 1024);
        } catch (TimeoutException e) {
            print("Server dump timed out.", e);
            Assert.fail("Server dump did not complete in " + LAUNCH_TIMEOUT + " seconds");
        } catch (Exception e) {
            print("Server dump failed.", e);
            Assert.fail(e.getMessage());
        } finally {
            if (zipFile != null && zipFile.exists())
                Assert.assertTrue("Could not delete server dump: " + zipFile.getAbsolutePath(), FileUtil.safeDelete(zipFile));
            stopServer();
            assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
        }
    }

    @Test
    public void testJavaDump() throws Exception {
        // server needs to be started for java dump to generate anything
        startServer();
        assertTrue("Server should be started", testServerState(IServer.STATE_STARTED));

        File dumpFolder = wsServerInfo.getServerOutputPath().toFile();
        String javaPlatform = System.getProperty("java.vendor");
        Pattern dumpFilePattern;
        if (javaPlatform != null && javaPlatform.contains("IBM"))
            dumpFilePattern = JAVA_DUMP_IBM_FILE_NAME_PATTERN;
        else
            dumpFilePattern = JAVA_DUMP_ORACLE_FILE_NAME_PATTERN;

        File[] dumpFiles = getFiles(dumpFolder, dumpFilePattern);
        if (dumpFiles != null) {
            for (File dumpFile : dumpFiles) {
                Assert.assertTrue("Could not delete prior java dump: " + dumpFile.getAbsolutePath(), FileUtil.safeDelete(dumpFile));
            }
        }

        File zipFile = null;
        try {
            ILaunch launch = wsRuntime.javadumpServer(wsServerInfo, null, null);
            int exitValue = waitForLaunch(launch);

            // java dump can fail when running on JRE instead of JDK
            Assert.assertTrue("Java dump did not exit normally. Ensure you are using a JDK instead of a JRE.  Exit value: " + exitValue, exitValue == 0);

            dumpFiles = getFiles(dumpFolder, dumpFilePattern);
            Assert.assertNotNull("Could not generate javadump for server " + wsServer.getServerName(), dumpFiles);
            Assert.assertTrue("Did not find any dump files", dumpFiles != null && dumpFiles.length > 0);
            zipFile = dumpFiles[0];
            Assert.assertTrue("Java dump suspiciously small (" + zipFile.length() + ") " + zipFile.getAbsolutePath(), zipFile.length() > 1024);
        } catch (TimeoutException e) {
            print("Java dump timed out.", e);
            Assert.fail("Java dump did not complete in " + LAUNCH_TIMEOUT + " seconds");
        } catch (Exception e) {
            print("Java dump failed.", e);
            Assert.fail(e.getMessage());
        } finally {
            if (zipFile != null && zipFile.exists())
                Assert.assertTrue("Could not delete java dump: " + zipFile.getAbsolutePath(), FileUtil.safeDelete(zipFile));
            stopServer();
            assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
        }
    }

    @Test
    public void testPluginConfig() {
        File pluginCfgFile = wsServerInfo.getServerOutputPath().append("plugin-cfg.xml").toFile();

        if (pluginCfgFile.exists())
            Assert.assertTrue("Could not delete prior plugin config: " + pluginCfgFile.getAbsolutePath(), FileUtil.safeDelete(pluginCfgFile));

        try {
            startServer();
            assertTrue("Server should be started", testServerState(IServer.STATE_STARTED));

            for (int i = 0; i < LAUNCH_TIMEOUT * 10; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
                try {
                    wsServerInfo.generatePluginConfig();
                    break;
                } catch (Exception e) {
                    // ignore
                }
            }

            if (!pluginCfgFile.exists())
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }

            Assert.assertTrue("Plugin config was not created: " + pluginCfgFile.getAbsolutePath(), pluginCfgFile.exists());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            try {
                stopServer();
                assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
            } catch (Exception e) {
                Assert.fail("Failure to stop server: " + e.getMessage());
            }

            if (pluginCfgFile.exists())
                Assert.assertTrue("Could not delete plugin config: " + pluginCfgFile.getAbsolutePath(), FileUtil.safeDelete(pluginCfgFile));
        }
    }

    @Test
    public void testInstallPackageAllZip() throws Exception {
        cleanUp();
        IPath newRuntime = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("newRuntime");
        deleteRuntime(newRuntime.toFile());
        File zipFile = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("packageTestAll.zip").toFile();
        MyDownloadHelper.myUnzip(zipFile, newRuntime);
        runtime = TestUtil.createRuntime(getServerTypeId(), newRuntime.toOSString(), "New_Liberty_Runtime");
        createServer(runtime, "New_Server", SERVER_LOCATION);
        startServer();
        String log = readConsole();
        assertTrue("Server should be started", testServerState(IServer.STATE_STARTED));
        assertTrue("FFDC generated , Start is not clean", !log.contains("FFDC"));
        stopServer();
        assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
        FileUtil.deleteDirectory(newRuntime.toOSString(), true);
    }

    @Test
    public void testInstallPackageMinifyZip() throws Exception {
        cleanUp();
        IPath newRuntime = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("newRuntime");
        deleteRuntime(newRuntime.toFile());
        File zipFile = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("packageTestMinify.zip").toFile();
        MyDownloadHelper.myUnzip(zipFile, newRuntime);
        runtime = TestUtil.createRuntime(getServerTypeId(), newRuntime.toOSString(), "New_Liberty_Runtime");
        createServer(runtime, "New_Server", SERVER_LOCATION);
        startServer();
        String log = readConsole();
        assertTrue("Server should be started", testServerState(IServer.STATE_STARTED));
        assertTrue("FFDC generated , Start is not clean", !log.contains("FFDC"));
        stopServer();
        assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
        FileUtil.deleteDirectory(newRuntime.toOSString(), true);
    }

    @Test
    public void testInstallUsrZip() throws Exception {
        cleanUp();
        IPath newRuntime = resourceFolder.append("UtilityTests/serverUtilsTestServer1");
        deleteRuntime(newRuntime.toFile());
        File zipFile = ResourcesPlugin.getWorkspace().getRoot().getLocation().append("packageTestUsr.zip").toFile();
        MyDownloadHelper.myUnzip(zipFile, newRuntime);
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER_NAME + "new", "resources/UtilityTests/serverUtilsTestServer1/usr/servers/serverUtilsTestServer");
        startServer();
        String log = readConsole();
        assertTrue("Server should be started", testServerState(IServer.STATE_STARTED));
        assertTrue("FFDC generated , Start is not clean", !log.contains("FFDC"));
        testPingWebPage("noisyservlet/NoisyServlet?text=BAZINGA!", "BAZINGA!");
        stopServer();
        assertTrue("Server should be stopped", testServerState(IServer.STATE_STOPPED));
        //To Do: delete newly created folder inside the test plug-in
        FileUtil.deleteDirectory(newRuntime.toOSString(), true);
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        FileUtil.safeDelete(ResourcesPlugin.getWorkspace().getRoot().getLocation().append("packageTestAll.zip").toFile());
        FileUtil.safeDelete(ResourcesPlugin.getWorkspace().getRoot().getLocation().append("packageTestUsr.zip").toFile());
        FileUtil.safeDelete(ResourcesPlugin.getWorkspace().getRoot().getLocation().append("packageTestMinify.zip").toFile());
        print("Ending test: UtilitiesTestCase\n");
    }

    protected File[] getFiles(File folder, final Pattern pattern) {
        return folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).find();
            }
        });
    }

    static class MyDownloadHelper extends DownloadHelper {
        static void myUnzip(File file, IPath path) {
            try {
                ArrayList<File> files = new ArrayList<File>();
                files.add(file);
                unzip(files, path, new NullProgressMonitor());
            } catch (CoreException e) {
                WLPCommonUtil.print("Exception when unzip the driver.");
                e.printStackTrace();
                Error err = new AssertionFailedError("Failed to unzip driver");
                err.initCause(e);
                throw err;
            }
        }
    }

    private void waitForZipFile(File zipFile, long length, int timeoutInSeconds) {
        int count = timeoutInSeconds * 2;
        for (int i = 0; i < count && zipFile.length() <= length; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    private void deleteRuntime(File runtime) {
        if (runtime.exists()) {
            try {
                FileUtil.deleteDirectory(runtime.getAbsolutePath(), true);
            } catch (IOException e) {
                print("Exception when trying to delete runtime: " + runtime.getAbsolutePath(), e);
            }
            assertFalse("The runtime should be deleted: " + runtime.getAbsolutePath(), runtime.exists());
        }
    }
}