/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.validation;

import java.io.IOException;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.validation.ValidatorMessage;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;

/**
 *
 */
public abstract class ValidationTestBase extends ToolsTestBase {

    protected static final String XML_MARKER = "org.eclipse.wst.xml.core.validationMarker";

    protected static IProject project;

    // Create a runtime and setup the server configurations
    protected void setupRuntime(String projectName, String resourcePath) throws Exception {
        createRuntime();
        project = WebSphereUtil.createUserProject(projectName, null, null);
        assertNotNull("Could not create user project.", project);
        assertTrue("User project does not exist.", project.exists());
        // Add the user directory to the runtime before importing the
        // config files to the project.  Otherwise any include files
        // will fail to resolve and then the dependencies will not be
        // set up properly.
        addUserDir(project);
        if (resourcePath != null)
            setupRuntimeServers(resourcePath);
    }

    // Lookup the IFile given a server name and a file name.
    protected IFile getServerFile(String serverName, String fileName) {
        return getServerFile(project, serverName, fileName);
    }

    protected IFile getServerFile(IProject project, String serverName, String fileName) {
        IFolder folder = project.getFolder("servers");
        folder = folder.getFolder(serverName);
        IFile file = folder.getFile(fileName);
        return file;
    }

    protected IFile getDefaultDropinsFile(String serverName, String fileName) {
        return getConfigDropinsFile(serverName, Constants.CONFIG_DEFAULT_DROPINS_FOLDER, fileName);
    }

    protected IFile getOverrideDropinsFile(String serverName, String fileName) {
        return getConfigDropinsFile(serverName, Constants.CONFIG_OVERRIDE_DROPINS_FOLDER, fileName);
    }

    protected IFile getConfigDropinsFile(String serverName, String dropinsFolder, String fileName) {
        IFolder folder = project.getFolder("servers");
        folder = folder.getFolder(serverName);
        folder = folder.getFolder(Constants.CONFIG_DROPINS_FOLDER);
        folder = folder.getFolder(dropinsFolder);
        IFile file = folder.getFile(fileName);
        return file;
    }

    protected IFile getFile(String serverName, String fileName, String path) {
        IFolder folder = project.getFolder("servers");
        folder = folder.getFolder(serverName);
        folder = folder.getFolder(path);
        IFile file = folder.getFile(fileName);
        return file;
    }

    // Check the number of messages is as expected.
    protected static void checkMessageCount(ValidatorMessage[] messages, int count) {
        if (messages.length != count) {
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < messages.length; i++) {
                if (i > 0)
                    str.append(", ");
                str.append((String) messages[i].getAttribute(IMarker.MESSAGE));
            }
            if (str.length() == 0)
                str.append("<none>");
            assertTrue("Expected " + count + " but got " + messages.length + " messages.  Messages are: " + str.toString(), false);
        }
    }

    // Find the message that the test is interested in checking
    protected static ValidatorMessage findMessage(ValidatorMessage[] messages, String text) {
        for (ValidatorMessage message : messages) {
            String msgText = (String) message.getAttribute(IMarker.MESSAGE);
            if (msgText.equals(text)) {
                return message;
            }
        }
        return null;
    }

    // Check that the message is as expected.
    protected static void checkMessage(ValidatorMessage message, String text, String location, int lineNumber, int severity) {
        checkMessage(message, text, location, lineNumber);
        int actualSeverity = ((Integer) message.getAttribute(IMarker.SEVERITY)).intValue();
        assertTrue("Severity should be : " + severity + ", but actual severity is: " + actualSeverity, severity == actualSeverity);
    }

    // Check that the message is as expected.
    protected static void checkMessage(ValidatorMessage message, String text, String location, int lineNumber) {
        String actualText = (String) message.getAttribute(IMarker.MESSAGE);
        assertTrue("The message text '" + actualText + "' does not match the expected text '" + text + "'", actualText.equals(text));
        checkMessageLocation(message, location, lineNumber);
    }

    protected static void checkMessage(ValidatorMessage message, String text, String splitString, String[] items, String location, int lineNumber) {
        String actualText = (String) message.getAttribute(IMarker.MESSAGE);
        int splitOffset = text.indexOf(splitString);
        String startText = text.substring(0, splitOffset);
        String endText = text.substring(splitOffset + splitString.length(), text.length());
        assertTrue("The message text '" + actualText + "' should start with '" + startText + "'", actualText.startsWith(startText));
        assertTrue("The message text '" + actualText + "' should end with '" + endText + "'", actualText.endsWith(endText));
        for (String item : items) {
            assertTrue("The message text '" + actualText + "' should contain '" + item + "'", actualText.contains(item));
        }
        checkMessageLocation(message, location, lineNumber);
    }

    protected static void checkMessageLocation(ValidatorMessage message, String location, int lineNumber) {
        IResource resource = message.getResource();
        assertTrue("The message resource is null.", resource != null);
        if (resource != null) {
            String actualLocation = resource.getLocation().toPortableString();
            assertTrue("Location '" + actualLocation + "' does not end with '" + location + "'", actualLocation.endsWith(location));
        }
        int actualLine = ((Integer) message.getAttribute(IMarker.LINE_NUMBER)).intValue();
        assertTrue("Line numbers don't match, expected: " + lineNumber + ", actual: " + actualLine, actualLine == lineNumber);
    }

    // Set up all of the runtime servers needed for this set of tests, copying
    // the folders and files from the test project to the runtime usr/servers directory.
    protected void setupRuntimeServers(String resourcePath) throws IOException {
        IPath fullResourcePath = resourceFolder.append(resourcePath);
        IFolder folder = project.getFolder(Constants.SERVERS_FOLDER);
        IPath projectPath = folder.getLocation();
        FileUtil.copyFiles(fullResourcePath.toPortableString(), projectPath.toPortableString());
        TestUtil.refreshResource(folder);
        TestUtil.jobWaitBuildandResource();
    }

    protected void setupRuntimeServer(String resourcePath, String serverName) throws Exception {
        IPath fullResourcePath = resourceFolder.append(resourcePath).append(serverName);
        IFolder folder = project.getFolder(Constants.SERVERS_FOLDER);
        folder = folder.getFolder(serverName);
        folder.create(true, true, new NullProgressMonitor());
        IPath projectPath = folder.getLocation();
        FileUtil.copyFiles(fullResourcePath.toPortableString(), projectPath.toPortableString());
        TestUtil.refreshResource(folder);
        forceBuild();
        getWebSphereRuntime().updateServerCache(true);
    }

    protected void deleteRuntimeServer(String serverName) throws CoreException {
        WebSphereServerInfo serverInfo = getServerInfo(serverName);
        if (serverInfo != null) {
            getWebSphereRuntime().deleteServer(serverInfo, new NullProgressMonitor());
        }
        forceBuild();
    }

    // Get the main configuration file for the server
    protected ConfigurationFile getConfigFile(String serverName) {
        WebSphereServerInfo serverInfo = getServerInfo(serverName);
        if (serverInfo != null)
            return serverInfo.getConfigRoot();
        return null;
    }

    // Get the configuration file for the server and URI
    protected ConfigurationFile getConfigFile(String serverName, URI uri) {
        WebSphereServerInfo serverInfo = getServerInfo(serverName);
        if (serverInfo != null)
            return serverInfo.getConfigurationFileFromURI(uri);
        return null;
    }

    protected WebSphereServerInfo getServerInfo(String serverName) {
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        UserDirectory userDir = wsRuntime.getUserDir(project.getName());
        WebSphereServerInfo serverInfo = wsRuntime.getServerInfo(serverName, userDir);
        return serverInfo;
    }
}
