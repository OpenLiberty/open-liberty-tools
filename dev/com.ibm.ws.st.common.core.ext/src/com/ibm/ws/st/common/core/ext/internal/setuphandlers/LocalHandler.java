/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.common.core.ext.internal.setuphandlers;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.ibm.ws.st.common.core.ext.internal.Trace;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformUtil.OperatingSystem;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine;
import com.ibm.ws.st.common.core.ext.internal.util.FileUtil;
import com.ibm.ws.st.common.core.ext.internal.util.ProcessHelper;

/**
 * Handler for executing commands and performing file operations locally. Creates a ProcessBuilder
 * to execute commands and returns a ProcessResult.
 */
public class LocalHandler extends AbstractPlatformHandler implements IPlatformHandler {

    // No public constructor. Use the PlatformHandlerFactory to get Platform Handlers
    LocalHandler() {}

    /** {@inheritDoc} */
    @Override
    public ExecutionOutput executeCommand(String command) throws Exception {
        ExecutionOutput execOut = runCommand(null, command, AbstractDockerMachine.DEFAULT_TIMEOUT, null);
        return execOut;
    }

    @Override
    public ExecutionOutput executeCommand(String command, long timeout) throws Exception {
        ExecutionOutput execOut = runCommand(null, command, timeout, null);
        return execOut;
    }

    @Override
    public ExecutionOutput executeCommand(Map<String, String> env, String command, long timeout) throws Exception {
        ExecutionOutput execOut = runCommand(env, command, timeout, null);
        return execOut;
    }

    @Override
    public ExecutionOutput executeCommand(Map<String, String> env, String command, long timeout, IProgressMonitor monitor) throws Exception {
        return runCommand(env, command, timeout, monitor);
    }

    protected ExecutionOutput runCommand(Map<String, String> cmdEnv, String cmdStr, long timeout, IProgressMonitor progressMonitor) throws Exception {
        String[] cmd;
        ProcessBuilder builder;

        //On MAC the full path of the command is required
        PlatformUtil.OperatingSystem os = getOS();
        if (os == PlatformUtil.OperatingSystem.MAC) {
            String macEnvPath = System.getProperty("com.ibm.ws.st.envPath", "/usr/local/bin/");
            StringBuilder path = new StringBuilder(macEnvPath);

            cmd = formatCommand(path.append(cmdStr).toString());
            builder = createProcessBuilder(cmdEnv, cmd);
            final Map<String, String> environment = builder.environment();

            //Need the environment variable PATH set on MAC
            environment.put("PATH", macEnvPath);
        }

        else {

            cmd = formatCommand(cmdStr);
            builder = createProcessBuilder(cmdEnv, cmd);
        }

        IProgressMonitor monitor = progressMonitor;
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        if (Trace.ENABLED) {
            StringBuilder sb = new StringBuilder("CMD: ");
            for (String c : cmd)
                sb.append(c + " ");
            Trace.trace(Trace.DETAILS, sb.toString());
        }

        Process process = builder.start();
        return ProcessHelper.waitForProcess(process, 100, timeout / 1000f, 300, monitor);
    }

    protected ProcessBuilder createProcessBuilder(Map<String, String> cmdEnv, String... options) {
        List<String> cmd = new ArrayList<String>();
        for (String option : options) {
            cmd.add(option);
        }
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(false);
        if (cmdEnv != null && !cmdEnv.isEmpty()) {
            Map<String, String> env = builder.environment();
            for (Map.Entry<String, String> entry : cmdEnv.entrySet()) {
                env.put(entry.getKey(), entry.getValue());
            }
        }
        return builder;
    }

    /*
     * Splits command string as string array with whitespace as delimiter.
     * Ignores spaces within quotes. File paths can have spaces within them.
     * eg docker cp /opt/ibm/wlp /home/user/websphere Application Server/usr
     * would return four arguments instead of six
     */
    private String[] formatCommand(String cmdStr) {
        List<String> cmdArray = new ArrayList<String>();
        Pattern pattern = Pattern.compile("[^\\s\"]+|\"[^\"]*\"");
        Matcher matcher = pattern.matcher(cmdStr);
        while (matcher.find()) {
            cmdArray.add(matcher.group().replaceAll("^\"|\"$", ""));
        }
        return cmdArray.toArray(new String[cmdArray.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public void startSession() throws ConnectException {
        // do nothing
    }

    /** {@inheritDoc} */
    @Override
    public boolean directoryExists(String path) throws ConnectException {
        File f = new File(path);
        return f.exists();
    }

    /** {@inheritDoc} */
    @Override
    public boolean fileExists(String filePath) throws ConnectException {
        File f = new File(filePath);
        return f.exists();
    }

    /** {@inheritDoc} */
    @Override
    public void createDirectory(String path) throws ConnectException, IOException {
        File f = new File(path);
        f.mkdirs();
    }

    /** {@inheritDoc} */
    @Override
    public void uploadFile(String sourcePath, String destinationPath) throws ConnectException, IOException {
        Path dest = new Path(destinationPath);
        File file = new File(sourcePath);
        FileUtil.copy(file.toURI().toURL(), dest);
    }

    /** {@inheritDoc} */
    @Override
    public void downloadFile(String sourcePath, String destinationPath) throws ConnectException, IOException {
        Path src = new Path(sourcePath);
        Path dest = new Path(destinationPath);
        FileUtil.copy(src.toFile().toURI().toURL(), dest);
    }

    /** {@inheritDoc} */
    @Override
    public void endSession() throws ConnectException {
        // do nothing
    }

    /** {@inheritDoc} */
    @Override
    public String getEnvValue(String key) throws ConnectException, IOException {
        return System.getProperty(key);
    }

    /** {@inheritDoc} */
    @Override
    public String getTempDir() throws IOException {
        return ensureEndsWithPathSeparator(System.getProperty("java.io.tmpdir"));
    }

    /** {@inheritDoc} */
    @Override
    public void deleteFile(String path) throws Exception {
        File f = new File(path);
        f.delete();
    }

    @Override
    public OperatingSystem getOS() throws ConnectException, IOException {
        String osName = getEnvValue("os.name");
        return PlatformUtil.getOS(osName);
    }

}
