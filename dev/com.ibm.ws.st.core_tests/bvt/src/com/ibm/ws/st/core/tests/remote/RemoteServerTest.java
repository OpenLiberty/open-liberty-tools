/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.remote;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.validation.internal.operations.ValidationBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.ICondition;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

/**
 *
 */
@TestCaseDescriptor(description = "Test remote server - general testing", isStable = true)
@RunWith(AllTests.class)
public class RemoteServerTest extends ToolsTestBase {

    // Do not change the runtime name as the application projects are targeted to this runtime
    protected static final String RUNTIME_NAME = "remoteServerTestRuntime";
    public static String BACKUP_FILE_POSTFIX = "_backup";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(RemoteServerTest.getOrderedTests());
        suite.setName(RemoteServerTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testServerJRELevel"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testCreateRemoteServer"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testServerStarted"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testModifyConfig"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testConfigFilesSync"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testConfigDeleteFile"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testAddWebModule"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testWebAppStarted"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testPingRemoteHTML"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testPingRemoteJSP"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testPingRemoteServlet"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testUpdateRemoteServlet"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testRemoveWebModule"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testAddWebModuleAgain"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testWebAppStartedAgain"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testPingRemoteServletAgain"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testUpdateRemoteServletAgain"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testRemoveWebModuleAgain"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "testConsoleLogFileDownloaded"));
        testSuite.addTest(TestSuite.createTest(RemoteServerTest.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        doRemoteSetup(RUNTIME_NAME);
    }

    protected IPath getUpdatedFileFolder() {
        return resourceFolder.append("RemoteServer/ServerConfigFiles");
    }

    @Test
    public void testServerJRELevel() throws Exception {
        jmxConnection = RemoteTestUtil.setJMXConnection(getRemoteHostName(), getRemotePort(), getRemoteUserName(), getRemotePassword());
        assertNotNull("JMX connection is null", jmxConnection);
        String serverJavaVersion = getJMXMBeanAttributeValue("java.lang:type=Runtime", "SystemProperties", "java.version");
        assertNotNull("Error getting java version for remote server", serverJavaVersion);
        print("Server java version: " + serverJavaVersion);
        String workspaceJavaVersion = System.getProperty("java.version");
        print("Workspace java version: " + workspaceJavaVersion);
        String[] sComps = serverJavaVersion.split("\\.");
        String[] wComps = workspaceJavaVersion.split("\\.");
        assertTrue("Workspace java version should be less than or equal to the remote server java version: " + workspaceJavaVersion + " <= " + serverJavaVersion,
                   Integer.parseInt(wComps[0]) <= Integer.parseInt(sComps[0]) && Integer.parseInt(wComps[1]) <= Integer.parseInt(sComps[1]));
    }

    @Test
    public void testCreateRemoteServer() throws Exception {
        createRemoteServer(runtime);
        createVM(JDK_NAME);
    }

    @Test
    public void testServerStarted() {
        assertTrue("Expected server state is " + IServer.STATE_STARTED + " but actual state is " + server.getServerState(), testServerState(IServer.STATE_STARTED));
    }

    @Test
    public void testModifyConfig() throws Exception {
        // Modify the main server.xml file and make sure the changes are mirrored

        //get file path
        IPath configFilePath = wsServer.getServerPath().append("server.xml");
        File configFile = configFilePath.toFile();
        assertNotNull(configFile);

        //edit file.. add a comment
        Document doc = FileUtil.getDOM(configFilePath.toOSString());
        assertNotNull(doc);
        Comment comment = doc.createComment("comment");
        NodeList nl = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeName().equals("featureManager")) {
                nl.item(i).getParentNode().insertBefore(comment, nl.item(i));
            }
        }

        FileUtil.saveDOM(doc, configFilePath.toOSString());

        boolean jobDone = TestUtil.jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
        assertTrue(jobDone);
        jobDone = TestUtil.jobWait(Constants.JOB_FAMILY);
        assertTrue(jobDone);

        // Get the DOM for the runtime file and check that the changes are there.
        doc = FileUtil.getDOM(configFilePath.toOSString());
        boolean found = false;

        NodeList newlist = doc.getDocumentElement().getChildNodes();

        for (int i = 0; i < newlist.getLength(); i++) {
            if (newlist.item(i).getNodeType() == Node.COMMENT_NODE) {
                Comment getComment = (Comment) newlist.item(i);
                found = getComment.getData().equals("comment");
                if (found) {
                    break;
                }
            }
        }
        assertTrue(found);
        print("Exiting Modify config test");
    }

    @Test
    public void testConfigFilesSync() throws Exception {

        IProject project = wsServer.getServerInfo().getUserDirectory().getProject();

        if (downloadedRemoteFiles != null) {
            for (IPath filePath : downloadedRemoteFiles) {
                if (filePath.getFileExtension().equals("env")) {
                    copyFile(getUpdatedFileFolder().append("/server.env"), filePath);
                } else if (filePath.getFileExtension().equals("options")) {
                    /*
                     * it can be one of:
                     *
                     * ${server.config.dir}/configDropins/defaults/jvm.options
                     * ${server.config.dir}/jvm.options
                     * ${server.config.dir}/configDropins/overrides/jvm.options
                     * $(wlp.install.dir}/usr/jvm.optoins
                     *
                     * By default, it should be put in the config folder
                     */
                    String strPath = filePath.toString();

                    if (strPath.contains(Constants.CONFIG_DEFAULT_DROPINS_FOLDER))
                        copyFile(getUpdatedFileFolder().append("/" + Constants.CONFIG_DROPINS_FOLDER + "/" + Constants.CONFIG_DEFAULT_DROPINS_FOLDER + "/"
                                                               + ExtendedConfigFile.JVM_OPTIONS_FILE),
                                 filePath);
                    else if (strPath.contains(Constants.CONFIG_OVERRIDE_DROPINS_FOLDER))
                        copyFile(getUpdatedFileFolder().append("/" + Constants.CONFIG_DROPINS_FOLDER + "/" + Constants.CONFIG_OVERRIDE_DROPINS_FOLDER + "/"
                                                               + ExtendedConfigFile.JVM_OPTIONS_FILE),
                                 filePath);
                    else if (strPath.contains(Constants.SHARED_FOLDER))
                        copyFile(getUpdatedFileFolder().append("/" + Constants.SHARED_FOLDER + "/" + ExtendedConfigFile.JVM_OPTIONS_FILE),
                                 filePath);
                    else
                        copyFile(getUpdatedFileFolder().append("/" + ExtendedConfigFile.JVM_OPTIONS_FILE), filePath);
                } else if (filePath.getFileExtension().equals("properties")) {
                    // don't replace bootstrap.properties file because the remote server ports are set by it
                    // copyFile(getUpdatedFileFolder().append("/bootstrap.properties"), filePath);
                } else if (filePath.getFileExtension().equals("xml") && filePath.lastSegment().equals("sampleInclude.xml")) {
                    copyFile(getUpdatedFileFolder().append("/sampleInclude.xml"), filePath);
                } else if (filePath.getFileExtension().equals("xml") && filePath.lastSegment().equals("default1.xml")) {
                    copyFile(getUpdatedFileFolder().append(Constants.CONFIG_DROPINS_FOLDER).append(Constants.CONFIG_DEFAULT_DROPINS_FOLDER).append("default1.xml"), filePath);
                } else if (filePath.getFileExtension().equals("xml") && filePath.lastSegment().equals("override1.xml")) {
                    copyFile(getUpdatedFileFolder().append(Constants.CONFIG_DROPINS_FOLDER).append(Constants.CONFIG_OVERRIDE_DROPINS_FOLDER).append("override1.xml"), filePath);
                }
            }

            project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

            waitForBuildToFinish();

            //wait for validation job to finish for the new files copied into the workspace
            TestUtil.waitForJobsToComplete(ValidationBuilder.FAMILY_VALIDATION_JOB);

            print("Testing the file changes");
            for (IPath filePath : downloadedRemoteFiles) {
                String fileExtension = filePath.getFileExtension();
                if (fileExtension.equals("env") || fileExtension.equals("properties") || fileExtension.equals("options")
                    || filePath.lastSegment().equals("sampleInclude.xml") || filePath.lastSegment().equals("default1.xml")
                    || filePath.lastSegment().equals("override1.xml")) {
                    String downloadTo = downloadFile(filePath);
                    assertTrue("Sync failed for file: " + downloadTo, testFileChange(downloadTo));
                }
            }
        }
    }

    @Test
    public void testConfigDeleteFile() throws Exception {
        IProject project = wsServer.getServerInfo().getUserDirectory().getProject();

        boolean envFileFound = false;
        if (downloadedRemoteFiles != null) {
            File envFile = null;
            for (IPath filePath : downloadedRemoteFiles) {
                if (filePath.getFileExtension().equals("env")) { //delete server.env file locally
                    envFile = filePath.toFile();
                    if (envFile.exists()) {
                        envFileFound = true;
                        // backup env file
                        FileUtil.copyFile(envFile.getAbsolutePath(), envFile.getAbsolutePath() + BACKUP_FILE_POSTFIX);
                        assertTrue(filePath + " was not deleted locally", envFile.delete());
                        print("Deleted file from local workspace: " + filePath);
                    }
                }
            }

            assertTrue("Server.env file was not found locally, cannot run the test", envFileFound);

            project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

            waitForBuildToFinish();

            boolean deletedFromRemote = false;

            checkDeleted: for (int i = 0; i < 5 && !deletedFromRemote; i++) {
                //test if it was deleted from remote server
                CompositeData[] listOfServerDirFiles = jmxConnection.getDirectoryEntries("${server.config.dir}", false, "");
                if (listOfServerDirFiles != null) {
                    for (CompositeData metaData : listOfServerDirFiles) {
                        String filePath = (String) metaData.get("fileName");
                        String[] list = filePath.split("/");
                        String fileName = list[list.length - 1];
                        if (fileName != null && (fileName.equals(ExtendedConfigFile.SERVER_ENV_FILE))) {
                            Thread.sleep(1000); // wait a sec before retrying
                            continue checkDeleted;
                        }
                    }
                }
                deletedFromRemote = true;
            }

            // restore backed up env file
            if (envFile != null && deletedFromRemote) {
                FileUtil.copyFile(envFile.getAbsolutePath() + BACKUP_FILE_POSTFIX, envFile.getAbsolutePath());
                project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
                project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

                waitForBuildToFinish();
            }
            assertTrue("File was not deleted from the remote server", deletedFromRemote);
        }
    }

    @Test
    public void testConsoleLogFileDownloaded() throws Exception {
        // test that console log file is downloaded, if it is then server console view should be working correctly
        String serverName = wsServer.getServerInfo().getServerName();
        IPath remoteUsrMetadataPath = wsServer.getWebSphereRuntime().getRemoteUsrMetadataPath().append(Constants.SERVERS_FOLDER).append(serverName);
        IPath consoleLog = remoteUsrMetadataPath.append("logs").append(Constants.CONSOLE_LOG);
        if (consoleLog == null) {
            fail("Console log is null");
            return;
        }

        boolean consoleLogGood = consoleLog.toFile().exists();

        assertTrue("Console log file doesn't exist", consoleLogGood);

        if (consoleLogGood) {
            long fileLength = 0;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(consoleLog.toFile());
                fileLength = fis.getChannel().size();
            } finally {
                if (fis != null)
                    fis.close();
            }
            assertTrue("Console log file was not downloaded correctly", fileLength > 0);
        }
    }

    public String downloadFile(IPath filePath) {
        String remoteUserDir = wsServer.getServerInfo().getUserDirectory().getRemoteUserPath().toOSString().replace("\\", "/"); //remote wlp/user dir
        String userDir = wsServer.getServerInfo().getUserDirectory().getPath().toOSString().replace("\\", "/"); //local wlp/user dir i.e workspace dir
        String downloadFrom = filePath.toOSString().replace("\\", "/");
        downloadFrom = downloadFrom.replace(userDir, remoteUserDir);
        // Since there are multiple jvm.options, need to organize them by the directories
        int segments = filePath.segmentCount();
        IPath downloadToPath = wsServer.getWebSphereServerBehaviour().getTempDirectory().append(filePath.removeFirstSegments(segments - 2));
        String downloadTo = downloadToPath.toOSString().replace("\\", "/");
        try {
            // Create folders if necessary
            IPath lastFolder = downloadToPath.removeLastSegments(1);
            File lastFile = lastFolder.toFile();
            if (!lastFile.exists())
                lastFile.mkdirs();

            jmxConnection.downloadFile(downloadFrom, downloadTo);
        } catch (Exception e) {
            print("Error downloading the file: " + downloadFrom);
        }
        return downloadTo;
    }

    public boolean testFileChange(String downloadTo) throws Exception {
        boolean flag = false;

        if (new Path(downloadTo).getFileExtension().equals("xml")) {
            Document freshDoc = FileUtil.getDOM(downloadTo);
            NodeList nl = freshDoc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.COMMENT_NODE) {
                    Comment getComment = (Comment) nl.item(i);
                    flag = getComment.getData().equals("comment");
                }
            }
        } else if (new Path(downloadTo).toString().endsWith("bootstrap.properties")) {
            // ignore bootstrap.properties file since it contains the remote port information
            // should be good enough to test the other property files
            flag = true;
        } else {
            BufferedReader br = new BufferedReader(new FileReader(downloadTo));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.equals("#comment"))
                    flag = true;
            }
            br.close();
        }
        return flag;
    }

    @Test
    public void testAddWebModule() throws Exception {
        //add app to server
        importProjects(new Path("/RemoteServer"), new String[] { "MyWeb", "MyWebEAR" });
        print("Waiting for build to finish");
        TestUtil.waitForJobsToComplete(ValidationBuilder.FAMILY_VALIDATION_JOB);
        TestUtil.jobWaitBuildandResource();
        print("Adding web project");
        addApp("MyWebEAR", false, 30);
    }

    @Test
    public void testWebAppStarted() throws Exception {
        testAppStarted("MyWebEAR");
    }

    @Test
    public void testPingRemoteHTML() throws Exception {
        String partialURL = "/MyWeb/test.html";
        testPingWebPage(partialURL, "HTML");
    }

    @Test
    public void testPingRemoteJSP() throws Exception {
        String partialURL = "/MyWeb/test.jsp";
        testPingWebPage(partialURL, "JSP");
    }

    @Test
    public void testPingRemoteServlet() throws Exception {
        String partialURL = "/MyWeb/Servlet1";
        testPingWebPage(partialURL, "Servlet");
    }

    // Test updating and then removing and then adding the app again, the content of the servlet
    // should stay the same after adding it back

    @Test
    public void testUpdateRemoteServlet() throws Exception {
        String partialURL = "/MyWeb/Servlet1";
        updateFile(getUpdatedFileFolder().append("Servlet1.java"),
                   getProject("MyWeb"), "src/com/ibm/test/Servlet1.java", 2000);
        testPingWebPage(partialURL, "new_S1");
    }

    @Test
    public void testRemoveWebModule() throws Exception {
        removeApp("MyWebEAR", 0);
        TestUtil.waitForJobsToComplete(ValidationBuilder.FAMILY_VALIDATION_JOB);
    }

    @Test
    public void testAddWebModuleAgain() throws Exception {
        //add app to server again
        print("Adding web project");
        addApp("MyWebEAR", false, 30);
    }

    @Test
    public void testWebAppStartedAgain() throws Exception {
        testAppStarted("MyWebEAR");
    }

    @Test
    public void testPingRemoteServletAgain() throws Exception {
        String partialURL = "/MyWeb/Servlet1";
        testPingWebPage(partialURL, "new_S1");
    }

    @Test
    public void testUpdateRemoteServletAgain() throws Exception {
        String partialURL = "/MyWeb/Servlet1";
        updateFile(getUpdatedFileFolder().append("Servlet1_2.java"),
                   getProject("MyWeb"), "src/com/ibm/test/Servlet1.java", 2000);
        testPingWebPage(partialURL, "new_S2");
    }

    @Test
    public void testRemoveWebModuleAgain() throws Exception {
        removeApp("MyWebEAR", 0);
        TestUtil.waitForJobsToComplete(ValidationBuilder.FAMILY_VALIDATION_JOB);
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        wait("Exiting test: RemoteServerTest", 1000);
    }

    /**
     * @param string
     */
    public void testAppStarted(final String moduleName) {
        final String serverName = server.getName();
        final boolean[] isStarted = { false };
        waitOnCondition("Waiting web app to start", 1000, 30000, new ICondition() {
            @Override
            public boolean isSatisfied() {
                for (IServer s : ServerCore.getServers()) {
                    if (s.getName().equals(serverName)) {
                        for (IModule module : s.getModules()) {
                            if (module.getName().equals(moduleName)) {
                                // wait for file to not exist, meaning the file lock is released
                                isStarted[0] = s.getModuleState(new IModule[] { module }) == IServer.STATE_STARTED;
                                return isStarted[0];
                            }
                        }
                    }
                }
                assertTrue("Couldn't get status of module: " + moduleName, false);
                return false;
            }
        });
        assertTrue(moduleName + " was not detected as started.", isStarted[0]);
    }
}
