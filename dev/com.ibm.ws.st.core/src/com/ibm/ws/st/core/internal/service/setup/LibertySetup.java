/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.service.setup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.w3c.dom.Document;

import com.ibm.ws.st.common.core.ext.internal.AbstractServerSetup;
import com.ibm.ws.st.common.core.ext.internal.Activator;
import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.Messages;
import com.ibm.ws.st.common.core.ext.internal.Trace;
import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.producer.AbstractServerProducer;
import com.ibm.ws.st.common.core.ext.internal.producer.ServerCreationException;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.core.internal.FeatureSet;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.core.internal.config.FeatureList.FeatureMapType;
import com.ibm.ws.st.core.internal.generation.Feature;
import com.ibm.ws.st.core.internal.generation.FeatureInfoHandler;

public class LibertySetup extends AbstractServerSetup {

    public static long DEFAULT_TIMEOUT = 10000;

    private IPlatformHandler setup = null;
    private String serviceType = null;
    protected Map<String, String> serviceInfo = null;
    private IRuntime rt = null;
    private File serverXML = null;
    protected boolean includesDownloaded = false;
    protected boolean configDropinsDownloaded = false;
    protected Map<File, String> includesMap = new HashMap<File, String>();
    protected Map<File, String> configDropinsMap = new HashMap<File, String>();
    protected ToUpdateTuple toUpdate = null;
    protected final Object configLock = this;

    @Override
    public void initialize(String serviceType, Map<String, String> serviceInfo, IRuntime runtime) throws ServerCreationException, UnsupportedServiceException {
        this.serviceType = serviceType;
        this.serviceInfo = serviceInfo;
        this.rt = runtime;
        if ("WASLibertyCorePlan".equals(serviceType)) {
            getPlatformHandler(PlatformHandlerFactory.PlatformType.SSH_KEYLESS);
        } else if ("LibertyDockerLocal".equals(serviceType)) {
            getPlatformHandler(PlatformHandlerFactory.PlatformType.DOCKER);
        } else {
            // Should not get here
            Trace.trace(Trace.WARNING, "Unsupported service type: " + serviceType);
        }

        // Init the setup handler
        // Currently this only impacts SSL, but should be generic
        startUp();
        // Init to fetch the server.xml
        getServerXML();

    }

    @SuppressWarnings("boxing")
    @Override
    public void setup(IProgressMonitor monitor) throws ServerCreationException, UnsupportedServiceException {
        SubMonitor mon = SubMonitor.convert(monitor, 100);

        try {
            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Environment setup started...");

            environmentSetup(setup);
            if (mon.isCanceled()) {
                return;
            }
            mon.worked(10);

            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Environment setup complete.");

            // #### START Setup IP table
            ArrayList<Integer> portsToAdd = new ArrayList<Integer>();

            // Default port for debug, this should be updated in the near future for dynamic detection
            portsToAdd.add(7777);

            try {
                if (serviceInfo.get(Constants.LIBERTY_HTTPS_PORT) != null) {
                    int httpsPort = Integer.parseInt(serviceInfo.get(Constants.LIBERTY_HTTPS_PORT));
                    portsToAdd.add(httpsPort);
                }
            } catch (NumberFormatException e) {
                Trace.logError("Error parsing the https port", e); //$NON-NLS-1
                throw new ServerCreationException(Messages.errorServerSetupFailed, e);
            }

            setupIPTables(setup, portsToAdd);
            if (mon.isCanceled()) {
                return;
            }
            mon.worked(30);

            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Finished firewall setup steps.");

            // #### END Setup IP table

            serverSetup(setup, mon.newChild(60));

            if (mon.isCanceled()) {
                return;
            }

            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Finished setup steps.");

        } catch (Exception e) {
            Trace.logError("Failed to setup", e); //$NON-NLS-1
            throw new ServerCreationException(Messages.errorServerSetupFailed, e);
        } finally {
            tearDown();
        }
    }

    private IPlatformHandler getPlatformHandler(PlatformType platformType) throws UnsupportedServiceException {
        if (setup != null)
            return setup;

        setup = PlatformHandlerFactory.getPlatformHandler(serviceInfo, platformType);
        return setup;
    }

    private void environmentSetup(IPlatformHandler setup) throws ServerCreationException {
        try {
            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Starting environment setup...");

            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Connecting remote session...");

            Path serverConfigPath = new Path(serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH));
            String defaultsConfigDropinsPath = serverConfigPath.append(Constants.DROPINS_DIR).append(Constants.DEFAULTS_DIR).toString();

            boolean found = setup.directoryExists(defaultsConfigDropinsPath);

            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Remote session started.");

            if (!found) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.DETAILS, Constants.DEFAULTS_DIR + " directory not found.");
                    Trace.trace(Trace.DETAILS, "Creating directory: " + defaultsConfigDropinsPath);
                }
                setup.createDirectory(defaultsConfigDropinsPath);
            }
            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Finished environment setup.");
        } catch (Exception e) {
            Trace.logError("Failed to setup environment", e);
            throw new ServerCreationException(Messages.errorServerSetupFailed, e);
        }
    }

    void serverSetup(IPlatformHandler setup, IProgressMonitor monitor) throws ServerCreationException {
        SubMonitor mon = SubMonitor.convert(monitor, 100);

        File remoteAdministrationTempFile = null;
        InputStream in = null;
        FileOutputStream fos = null;

        if (Trace.ENABLED)
            Trace.trace(Trace.DETAILS, "Starting server setup steps.");
        if (Trace.ENABLED)
            Trace.trace(Trace.DETAILS, "Copying remote administration config...");

        try {
            URL remoteAdministrationFileURL = Activator.getInstance().getBundle().getEntry("config/" + Constants.LIBERTY_REMOTE_ADMINISTRATION_NAME);

            in = remoteAdministrationFileURL.openStream();
            String[] prefixSuffix = Constants.LIBERTY_REMOTE_ADMINISTRATION_NAME.split("\\.");
            remoteAdministrationTempFile = File.createTempFile(prefixSuffix[0], prefixSuffix[1]);
            fos = new FileOutputStream(remoteAdministrationTempFile);
            byte[] buffer = new byte[16384];
            int len;
            while ((len = in.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (Exception e) {
            Trace.logError("Failed to retrieve configuration file from plugin.", e);
            throw new ServerCreationException(Messages.errorFailedRemoteConfigUpload, e);
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (Exception ex) {
                // ignore
            }
            try {
                if (in != null)
                    in.close();
            } catch (Exception ex) {
                // ignore
            }
        }

        if (mon.isCanceled()) {
            return;
        }
        mon.worked(20);

        try {
            String restConnectorFeature = resolveFeature(setup, serviceInfo, "restConnector");

            if (mon.isCanceled()) {
                return;
            }
            mon.worked(30);

            Path serverConfigPath = new Path(serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH));
            ConfigUtils.updateRemoteFileAccess(ConfigUtils.documentLoad(remoteAdministrationTempFile), remoteAdministrationTempFile,
                                               setup.getEnvValue(com.ibm.ws.st.core.internal.Constants.ENV_LOG_DIR), restConnectorFeature);

            // if an old configuration file exists, remove it.
            String oldPath = serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH) + Constants.LIBERTY_DEFAULTS_PATH + Constants.LIBERTY_OLD_REMOTE_ADMINISTRATION_NAME;
            if (setup.fileExists(oldPath)) {
                setup.deleteFile(oldPath);
            }

            setup.uploadFile(remoteAdministrationTempFile.getAbsolutePath(),
                             serverConfigPath.append(Constants.LIBERTY_DEFAULTS_PATH + Constants.LIBERTY_REMOTE_ADMINISTRATION_NAME).toString());

            if (mon.isCanceled()) {
                return;
            }
            mon.worked(30);
        } catch (Exception e) {
            Trace.logError("Failed to upload remote administration config", e);
            throw new ServerCreationException(Messages.errorFailedRemoteConfigUpload, e);
        } finally {
            try {
                if (remoteAdministrationTempFile != null)
                    remoteAdministrationTempFile.delete();
            } catch (Exception ex) {
                // ignore
            }
        }

        try {
            String command = getStartCommand(serviceInfo);
            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Starting server using command: " + command);

            ExecutionOutput output = setup.executeCommand(command);
            if (Trace.ENABLED) {
                int returnCode = output.getReturnCode();
                Trace.trace(Trace.DETAILS, "Return code: " + returnCode);
                Trace.trace(Trace.DETAILS, output.getOutput());
                Trace.trace(Trace.DETAILS, output.getError());
            }

        } catch (Exception e) {
            Trace.logError("Failed to upload remote administration config", e);
            throw new ServerCreationException(Messages.errorFailedRemoteConfigUpload, e);
        }

        if (mon.isCanceled()) {
            return;
        }
        mon.worked(20);
    }

    public static String resolveFeature(IPlatformHandler handler, Map<String, String> serviceInfo, String featureName) {
        if (featureName == null)
            return null;
        IPath featureManagerScript = new Path(serviceInfo.get(Constants.LIBERTY_RUNTIME_INSTALL_PATH)).append("/bin/featureManager");
        IPath featureListJar = new Path(serviceInfo.get(Constants.LIBERTY_RUNTIME_INSTALL_PATH)).append("/bin/tools/ws-featurelist.jar");
        IPath featureListFile = new Path(serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH)).append("/st_featureList_st.xml");

        try {
            if (handler.fileExists(featureListFile.toString())) {
                handler.deleteFile(featureListFile.toString());
            }

            // If the featureManager script does not exist use the feature list jar
            String command = "java -jar " + featureListJar + " " + featureListFile;
            try {
                // Check if the featureManager exists
                if (handler.fileExists(featureManagerScript.toString())) {
                    command = featureManagerScript + " featureList " + featureListFile;
                }
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not determine if the feature manager script exists: " + featureManagerScript.toString(), e);
            }

            // the output return code was found to be unreliable so we just check the error stream instead
            ExecutionOutput output = handler.executeCommand(command, 30000);
            if (output.getError() != null && output.getError().length() > 0) {
                throw new IOException("Failed to generate feature list: " + output.getError());
            }
            File tempFeatureListFile = File.createTempFile("featureList", ".xml");
            handler.downloadFile(featureListFile.toString(), tempFeatureListFile.getAbsolutePath());

            if (tempFeatureListFile.exists()) {
                InputStream is = null;
                try {
                    is = new FileInputStream(tempFeatureListFile);
                    HashMap<FeatureMapType, HashMap<String, Feature>> map = FeatureInfoHandler.parseFeatureListXML(is);
                    HashMap<String, Feature> featureMap = map.get(FeatureList.FeatureMapType.PUBLIC_FEATURES_KEYED_BY_NAME);
                    return FeatureSet.resolve(featureName, featureMap.keySet().toArray(new String[featureMap.size()]));
                } catch (Throwable t) {
                    Trace.logError("Error parsing featurelist file: " + tempFeatureListFile.getAbsolutePath(), t);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Error closing featurelist file: " + tempFeatureListFile.getAbsolutePath(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("Rest Connector feature detection failed", e);
        } finally {
            try {
                if (handler.fileExists(featureListFile.toString())) {
                    handler.deleteFile(featureListFile.toString());
                }
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.logError("Failed to delete the generated feature list file", e);
            }
        }
        return null;
    }

    // For setting up the firewall rules

    @SuppressWarnings("boxing")
    private void setupIPTables(IPlatformHandler setup, List<Integer> ports) throws Exception {

        List<Integer> inputPortsToChange = new ArrayList<Integer>();
        List<Integer> outputPortsToChange = new ArrayList<Integer>();

        boolean rulesChanged = false;

        String checkRuleHeader = "iptables-save | grep "; //$NON-NLS-1
        String inputRuleCheckHeader = "-A INPUT -p tcp"; //$NON-NLS-1
        String dportCheck = "--dport"; //$NON-NLS-1
        String outputRuleCheckHeader = "-A OUTPUT -p tcp"; //$NON-NLS-1
        String sportCheck = "--sport"; //$NON-NLS-1
        String acceptRule = " -j ACCEPT"; //$NON-NLS-1

        String multi = "--match multiport "; //$NON-NLS-1

        String inputRuleHeader = "iptables -I INPUT -p tcp "; //$NON-NLS-1
        String outputRuleHeader = "iptables -I OUTPUT -p tcp "; //$NON-NLS-1

        String saveIPTable = "service iptables save"; //$NON-NLS-1
        String restartIPTable = "service iptables restart"; //$NON-NLS-1

        String regex = "\\r?\\n"; //$NON-NLS-1

        for (int port : ports) {
            String checkRule = checkRuleHeader + port;
            ExecutionOutput output = setup.executeCommand(checkRule);

            // Check line by line to see if the input/output rules has the mentioned port
            boolean inputContains = false;
            boolean outputContains = false;

            String[] outputLines = output.getOutput().split(regex);
            for (String line : outputLines) {
                if (line.contains(String.valueOf(port)) && line.contains(acceptRule)) {
                    if (line.contains(inputRuleCheckHeader) && line.contains(dportCheck)) {
                        inputContains = true;
                    }
                    if (line.contains(outputRuleCheckHeader) && line.contains(sportCheck)) {
                        outputContains = true;
                    }
                }
            }
            if (!inputContains) {
                inputPortsToChange.add(port);
            }
            if (!outputContains) {
                outputPortsToChange.add(port);
            }

        }

        // Update the input rules

        // Note: must use -I to insert instead of -A to append
        // Because iptables works top to bottom, if there's a rule before that to block
        // Then execution flow will just break there, -I adds the rule at the top of the table

        if (inputPortsToChange.size() > 0) {
            StringBuilder inputChange = new StringBuilder();
            inputChange.append(inputRuleHeader);
            boolean multiInput = inputPortsToChange.size() > 1;

            if (multiInput) {
                inputChange.append(multi);
            }
            String portArg = multiInput ? "--dports " : "--dport "; //NON-NLS-1 //NON-NLS-2
            inputChange.append(portArg);
            String inputPort = ""; //$NON-NLS-1
            for (int port : inputPortsToChange) {
                if (inputPort == "") { //$NON-NLS-1
                    inputPort = String.valueOf(port);
                } else {
                    inputPort = inputPort + "," + String.valueOf(port); //$NON-NLS-1
                }
            }
            inputChange.append(inputPort);
            inputChange.append(acceptRule);

            setup.executeCommand(inputChange.toString());
            rulesChanged = true;
        }

        if (outputPortsToChange.size() > 0) {
            StringBuilder outputChange = new StringBuilder();
            outputChange.append(outputRuleHeader);
            boolean multiOutput = outputPortsToChange.size() > 1;

            if (multiOutput) {
                outputChange.append(multi);
            }
            String portArg = multiOutput ? "--sports " : "--sport "; //NON-NLS-1 //NON-NLS-2
            outputChange.append(portArg);
            String outputPort = ""; //$NON-NLS-1
            for (int port : outputPortsToChange) {
                if (outputPort == "") { //$NON-NLS-1
                    outputPort = String.valueOf(port);
                } else {
                    outputPort = outputPort + "," + String.valueOf(port); //$NON-NLS-1
                }
            }
            outputChange.append(outputPort);
            outputChange.append(acceptRule);

            setup.executeCommand(outputChange.toString());
            rulesChanged = true;
        }

        if (rulesChanged) {
            setup.executeCommand(saveIPTable);
            setup.executeCommand(restartIPTable);
        }
    }

    private String getStartCommand(Map<String, String> serviceInfo) {
        StringBuilder sb = new StringBuilder();

        Path serverInstallPath = new Path(serviceInfo.get(Constants.LIBERTY_RUNTIME_INSTALL_PATH));
        String execPath = serverInstallPath.append("bin").append("server").toString();
        String commandArgs[] = { "start", serviceInfo.get(Constants.LIBERTY_SERVER_NAME) };

        // surround executable command with quotes
        sb.append("\"");
        sb.append(execPath);
        sb.append("\"");

        // separate command arguments with spaces
        for (String arg : commandArgs) {
            sb.append(" ");
            sb.append(arg);
        }
        return sb.toString();
    }

    @Override
    public void createServer() throws ServerCreationException, UnsupportedServiceException {
        if (Trace.ENABLED)
            Trace.trace(Trace.DETAILS, "Creating server...");

        String runtimeType = rt.getRuntimeType().getId();
        AbstractServerProducer producer = Activator.getServerProducer(runtimeType);
        if (producer == null)
            throw new UnsupportedServiceException(NLS.bind(Messages.errorNoServerProducersFound, serviceType));

        producer.createServer(rt, serviceInfo);

        if (Trace.ENABLED)
            Trace.trace(Trace.DETAILS, "Finished creating server.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateRemoteSecurity(String user, String password, int code, IProgressMonitor monitor) {
        synchronized (configLock) {
            if (setup != null) {
                try {

                    monitor.worked(10);
                    if (monitor.isCanceled()) {
                        return;
                    }

                    if (toUpdate != null) {
                        // Use includes or configDropins
                        File tempXML = toUpdate.tempFile;
                        Document doc = ConfigUtils.documentLoad(tempXML);
                        // upload the modified file back to Docker
                        ConfigUtils.updateRemoteSecurity(doc, tempXML, user, password, code);

                        if (monitor.isCanceled()) {
                            return;
                        }
                        setup.uploadFile(tempXML.getAbsolutePath(), toUpdate.destinationPath);
                        monitor.worked(40);

                    } else {
                        // upload a predefined defaults xml back to Docker
                        Path serverConfigPath = new Path(serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH));
                        // if the tail has a trailing separator, the result will have a trailing separator.
                        String defaultsConfigDropinsPath = serverConfigPath.append(Constants.LIBERTY_DEFAULTS_PATH).toString();
                        if (!setup.directoryExists(defaultsConfigDropinsPath)) {
                            setup.createDirectory(defaultsConfigDropinsPath);
                        }
                        if (monitor.isCanceled()) {
                            return;
                        }

                        String oldBasicRegConfig = defaultsConfigDropinsPath + Constants.LIBERTY_OLD_BASIC_REGISTRY_NAME;
                        if (setup.fileExists(oldBasicRegConfig)) {
                            setup.deleteFile(oldBasicRegConfig);
                        }

                        setup.uploadFile(ConfigUtils.createBasicRegConfig(user, password).getAbsolutePath(),
                                         defaultsConfigDropinsPath + Constants.LIBERTY_BASIC_REGISTRY_NAME);
                        monitor.worked(40);

                    }
                } catch (Exception e) {
                    Trace.logError("Error updaing the server config files", e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int validateRemoteSecurity(String user, String password, IProgressMonitor mon) {
        int result = -1;
        int serverResult = -1;
        int includeResult = -1;
        int configDropinResult = -1;
        Document doc = null;

        SubMonitor monitor = SubMonitor.convert(mon, 100);

        // Parse the server.xml and includes and configDropins
        // if the the above files do not contain a basicRegistry or the basicRegistry does not have any users defined in it, return 1
        // if any contains the basicRegistry and 1 or more users in it, but the user specified is not found, return 2
        // if any contains the basicRegistry and 1 or more users in it, and the user specified is listed, but its password does not match, return 4
        // if any contains the basicRegistry and 1 or more users in it, and the user specified is listed, and the password matches, but it does not have admin role, return 8
        // if any contains the basicRegistry and 1 or more users in it, and the user specified is listed, the password matches, and the user has admin role, return 0

        if (setup != null) {
            if (serverXML != null) {
                try {
                    doc = ConfigUtils.documentLoad(serverXML);
                    if (monitor.isCanceled()) {
                        return -1;
                    }
                    monitor.worked(10);
                    if (doc != null) {
                        serverResult = ConfigUtils.validateRemoteSecurity(doc, user, password);
                        if (serverResult == 0) { // As soon as one user security matches, we can proceed with connecting
                            return 0;
                        }

                        // Compare the results from parsing the server.xml, all of the includes and all of the configDropins
                        if (serverResult > result) {
                            result = serverResult;
                        }
                    }
                    if (monitor.isCanceled()) {
                        return -1;
                    }
                    monitor.worked(10);

                    // Parse the includes
                    getIncludes();

                    if (monitor.isCanceled()) {
                        return -1;
                    }
                    monitor.worked(20);

                    for (Map.Entry<File, String> entry : includesMap.entrySet()) {
                        File tempFile = entry.getKey();
                        if (tempFile.exists() && tempFile.getTotalSpace() > 0) {
                            Document tempDoc = ConfigUtils.documentLoad(tempFile);
                            int current = ConfigUtils.validateRemoteSecurity(tempDoc, user, password);
                            if (current == 0) { // As soon as one user security matches, we can proceed with connecting
                                return 0;
                            }
                            if (current > includeResult) {
                                includeResult = current;
                                if (includeResult > result) { // Compare the results from parsing the server.xml, all of the includes and all of the configDropins
                                    result = includeResult;
                                    toUpdate = new ToUpdateTuple(tempFile, entry.getValue());
                                }
                            }
                        }
                    }

                    if (monitor.isCanceled()) {
                        return -1;
                    }
                    monitor.worked(10);

                    // Parse the configDropins
                    getConfigDropins();

                    if (monitor.isCanceled()) {
                        return -1;
                    }
                    monitor.worked(20);

                    for (Map.Entry<File, String> entry : configDropinsMap.entrySet()) {
                        File tempFile = entry.getKey();
                        if (tempFile.exists() && tempFile.getTotalSpace() > 0) {
                            Document tempDoc = ConfigUtils.documentLoad(tempFile);
                            int current = ConfigUtils.validateRemoteSecurity(tempDoc, user, password);
                            if (current == 0) { // As soon as one user security matches, we can proceed with connecting
                                return 0;
                            }
                            if (current > configDropinResult) {
                                configDropinResult = current;
                                if (configDropinResult > result) { // Compare the results from parsing the server.xml, all of the includes and all of the configDropins
                                    result = configDropinResult;
                                    toUpdate = new ToUpdateTuple(tempFile, entry.getValue());
                                }
                            }
                        }
                    }

                    if (monitor.isCanceled()) {
                        return -1;
                    }
                    monitor.worked(30);

                } catch (Exception e) {
                    Trace.logError("Error validating user security", e);
                }
            }
        }

        return result;
    }

    private class ToUpdateTuple {
        protected File tempFile;
        protected String destinationPath;

        public ToUpdateTuple(File file, String path) {
            this.tempFile = file;
            this.destinationPath = path;
        }
    }

    /** {@inheritDoc} */
    @Override
    public File getServerXML() {
        File tempServerFile = null;
        if (serverXML == null) {
            try {
                if (setup != null) {
                    tempServerFile = File.createTempFile("server", ".xml");
                    if (tempServerFile != null) {
                        Path serverConfigPath = new Path(serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH));
                        setup.downloadFile(serverConfigPath.append("/server.xml").toString(), tempServerFile.getCanonicalPath());
                        serverXML = tempServerFile;
                    }

                }
            } catch (Exception e) {
                Trace.logError("Error getting the server.xml from the remote environment", e);
            }
        }
        return serverXML;
    }

    private void getIncludes() {
        if (includesDownloaded) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.DETAILS, "Include files already downloaded, skipping");
            }
            return;
        }
        Document doc = null;
        try {
            doc = ConfigUtils.documentLoad(serverXML);
            // Check the includes
            ArrayList<String> includes = ConfigUtils.getIncludes(doc);
            for (String include : includes) {
                boolean referenceInInclude = ConfigVarsUtils.containsReference(include);

                if (referenceInInclude) {
                    ConfigVars cv = new ConfigVars();
                    setVars(cv);
                    String resolvedInclude = cv.resolve(include);
                    if (setup != null && setup.fileExists(resolvedInclude)) {
                        File tempFile = File.createTempFile("include", ".xml");
                        setup.downloadFile(resolvedInclude, tempFile.getCanonicalPath());
                        includesMap.put(tempFile, resolvedInclude);
                    }
                } else {
                    // Relative
                    String relativePath = serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH) + "/" + include;
                    if (setup != null && setup.fileExists(relativePath)) {
                        File tempFile = File.createTempFile("include", ".xml");
                        setup.downloadFile(relativePath, tempFile.getCanonicalPath());
                        includesMap.put(tempFile, relativePath);
                    }
                }
                includesDownloaded = true;
            }
        } catch (Exception e) {
            Trace.logError("Error getting the include files from the remote environment", e);
        }
    }

    private void getConfigDropins() {
        if (configDropinsDownloaded) {
            return;
        }
        String regex = "\\r?\\n";
        Path serverConfigPath = new Path(serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH));
        String defaultsConfigDropinsPath = serverConfigPath.append(Constants.DROPINS_DIR).append(Constants.DEFAULTS_DIR).toString();
        String overridesConfigDropinsPath = serverConfigPath.append(Constants.DROPINS_DIR).append(Constants.OVERRIDES_DIR).toString();

        if (setup != null) {
            try {
                if (setup.directoryExists(defaultsConfigDropinsPath)) {
                    String output = setup.executeCommand("ls " + defaultsConfigDropinsPath).getOutput();
                    String files[] = output.split(regex);
                    for (int i = 0; i < files.length; i++) {
                        String filePath = defaultsConfigDropinsPath + "/" + files[i];
                        if (setup.fileExists(filePath)) {
                            File tempFile = File.createTempFile("defaultConfig", ".xml");
                            setup.downloadFile(filePath, tempFile.getCanonicalPath());
                            configDropinsMap.put(tempFile, filePath);
                        }
                    }
                }
                if (setup.directoryExists(overridesConfigDropinsPath)) {
                    String output = setup.executeCommand("ls " + overridesConfigDropinsPath).getOutput();
                    String files[] = output.split(regex);
                    for (int i = 0; i < files.length; i++) {
                        String filePath = overridesConfigDropinsPath + "/" + files[i];
                        if (setup.fileExists(filePath)) {
                            File tempFile = File.createTempFile("overridesConfig", ".xml");
                            setup.downloadFile(filePath, tempFile.getCanonicalPath());
                            configDropinsMap.put(tempFile, filePath);
                        }

                    }
                }
                configDropinsDownloaded = true;
            } catch (Exception e) {
                Trace.logError("Error getting the configDropins files from the remote environment", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getHTTPPort(String portType) {
        int result = -1;
        Document doc = null;
        try {
            doc = ConfigUtils.documentLoad(serverXML);
            if (portType.equals(Constants.LIBERTY_HTTP_PORT))
                result = ConfigUtils.getHTTPPort(doc, com.ibm.ws.st.core.internal.Constants.HTTP_PORT);
            else if (portType.equals(Constants.LIBERTY_HTTPS_PORT)) {
                result = ConfigUtils.getHTTPPort(doc, com.ibm.ws.st.core.internal.Constants.HTTPS_PORT);
            }
        } catch (Exception e) {
            Trace.logError("Error obtaining the HTTP/HTTPs port", e);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void updateServiceInfo(String key, String value) {
        if (serviceInfo == null) {
            return;
        }

        serviceInfo.put(key, value);

    }

    // Docker specific
    private void setVars(ConfigVars cv) {

        String serverConfigDir = serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH);
        String installDir = serviceInfo.get(Constants.LIBERTY_RUNTIME_INSTALL_PATH);

        cv.addResolved(ConfigVars.WLP_USER_DIR, installDir + "/usr", null, null);
        cv.addResolved(ConfigVars.SHARED_APP_DIR, installDir + "/usr/shared/apps", null, null);
        cv.addResolved(ConfigVars.SHARED_CONFIG_DIR, installDir + "usr/shared/config", null, null);
        cv.addResolved(ConfigVars.SHARED_RESOURCE_DIR, installDir + "usr/shared/resources", null, null);
        cv.addResolved(ConfigVars.SERVER_CONFIG_DIR, serverConfigDir, null, null);
        cv.addResolved(ConfigVars.SERVER_OUTPUT_DIR, serverConfigDir, null, null);

    }

    private void startUp() throws ServerCreationException {
        try {
            setup.startSession();
        } catch (Exception e) {
            Trace.logError("Failed to connect", e);
            throw new ServerCreationException(NLS.bind(Messages.errorFailedRemoteConnection, serviceInfo.get(Constants.HOSTNAME)), e);
        }
    }

    private void tearDown() {
        try {
            setup.endSession();
            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Remote session ended.");
        } catch (Exception e2) {
            // intentionally ignored
        }
    }

}
